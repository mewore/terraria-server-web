import { fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { AccountTypeEntity, LoginModel, SessionViewModel, SignupModel } from 'src/generated/backend';
import { AuthenticatedUser } from '../types';
import { AuthenticationStateService, SessionState } from './authentication-state.service';
import { AuthenticationStateServiceStub } from './authentication-state.service.stub';

import { AuthenticationService, AuthenticationServiceImpl } from './authentication.service';
import { ErrorService } from './error.service';
import { ErrorServiceStub } from './error.service.stub';
import { RestApiService } from './rest-api.service';
import { RestApiServiceStub } from './rest-api.service.stub';
import { StorageService } from './storage.service';
import { StorageServiceStub } from './storage.service.stub';

describe('AuthenticationService', () => {
    let service: AuthenticationService;

    let restApiService: RestApiService;

    let authenticationStateService: AuthenticationStateService;
    let unsureSubject: Subject<void>;

    let storageService: StorageService;
    let errorService: ErrorService;

    let userSubscription: Subscription | undefined;
    let emittedUsers: (AuthenticatedUser | undefined)[];

    function instantiate(): AuthenticationService {
        service = TestBed.inject(AuthenticationServiceImpl);
        userSubscription = service.userObservable.subscribe({
            next: (newUser) => emittedUsers.push(newUser),
        });
        return service;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            providers: [
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: AuthenticationStateService, useClass: AuthenticationStateServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
                { provide: StorageService, useClass: StorageServiceStub },
            ],
        }).compileComponents();

        restApiService = TestBed.inject(RestApiService);

        authenticationStateService = TestBed.inject(AuthenticationStateService);
        unsureSubject = new BehaviorSubject<void>(undefined);
        spyOnProperty(authenticationStateService, 'unsureObservable', 'get').and.returnValue(
            unsureSubject.asObservable()
        );

        storageService = TestBed.inject(StorageService);
        errorService = TestBed.inject(ErrorService);

        emittedUsers = [];
    });

    afterEach(() => {
        userSubscription?.unsubscribe();
        (service as AuthenticationServiceImpl).ngOnDestroy();
    });

    it('should be created', () => {
        expect(instantiate()).toBeTruthy();
    });

    describe('when there is initially no user in the session storage', () => {
        it('should have no user', () => {
            expect(instantiate().currentUser).toBeUndefined();
        });

        it('canManageHosts should return false', () => {
            expect(service.canManageHosts).toBeFalse();
        });

        describe('when there is an unsure notification', () => {
            let pingSpy: jasmine.Spy<() => void>;

            beforeEach(() => {
                instantiate();
                pingSpy = spyOn(restApiService, 'ping');
            });

            it('should not ping', () => {
                unsureSubject.next();
                expect(pingSpy).not.toHaveBeenCalled();
            });
        });
    });

    describe('when there is initially a user in the storage', () => {
        let markAsUnsureSpy: jasmine.Spy<() => void>;

        beforeEach(() => {
            storageService.user = {
                authData: 'authData',
                username: 'username',
            } as AuthenticatedUser;
            markAsUnsureSpy = spyOn(authenticationStateService, 'markAsUnsure').and.returnValue();
            instantiate();
        });

        it('should have the user', () => {
            expect(service.currentUser?.username).toBe('username');
        });

        it('should have populated the authData', () => {
            expect(authenticationStateService.authData).toBe('authData');
        });

        it('should have marked the authentication state as unsure', () => {
            expect(markAsUnsureSpy).toHaveBeenCalled();
        });

        it('should not emit the user', () => {
            expect(emittedUsers.length).toBe(0);
        });

        describe('when there is an unsure notification', () => {
            let pingSpy: jasmine.Spy<() => Promise<AccountTypeEntity>>;

            beforeEach(() => {
                instantiate();
                pingSpy = spyOn(restApiService, 'ping');
            });

            it('should ping', () => {
                unsureSubject.next();
                expect(pingSpy).toHaveBeenCalledWith();
            });

            describe('when the ping succeeds', () => {
                let accountType: AccountTypeEntity;

                beforeEach(fakeAsync(() => {
                    accountType = {} as AccountTypeEntity;
                    pingSpy = pingSpy.and.resolveTo(accountType);
                    unsureSubject.next();
                    flushMicrotasks();
                }));

                it('should save the received account type', () => {
                    expect(service.currentUser?.accountType).toBe(accountType);
                });

                it('should mark the session as authenticated', () => {
                    expect(authenticationStateService.sessionState).toBe(SessionState.AUTHENTICATED);
                });

                describe('when the user is able to manage hosts', () => {
                    beforeEach(() => {
                        accountType.ableToManageHosts = true;
                    });

                    it('canManageHosts should return true', () => {
                        expect(service.canManageHosts).toBeTrue();
                    });
                });
            });

            describe('when the ping fails', () => {
                let errorSpy: jasmine.Spy<(error: Error) => void>;
                let error: Error;

                beforeEach(fakeAsync(() => {
                    errorSpy = spyOn(errorService, 'showError').and.returnValue();
                    error = new Error('oof');
                    pingSpy = pingSpy.and.rejectWith(error);
                    unsureSubject.next();
                    flushMicrotasks();
                }));

                it('should report the error', () => {
                    expect(errorSpy).toHaveBeenCalledOnceWith(error);
                    unsureSubject.next();
                    expect(service.currentUser).toBeUndefined();
                });

                it('should remove the current user', () => {
                    unsureSubject.next();
                    expect(service.currentUser).toBeUndefined();
                });

                it('should mark the session as unauthenticated', () => {
                    unsureSubject.next();
                    expect(authenticationStateService.sessionState).toBe(SessionState.UNAUTHENTICATED);
                });
            });
        });
    });

    describe('logging in', () => {
        let logInSpy: jasmine.Spy<(model: LoginModel) => Promise<SessionViewModel>>;

        beforeEach(async () => {
            logInSpy = spyOn(restApiService, 'logIn').and.resolveTo({
                token: 'token',
            } as SessionViewModel);
            instantiate();
            await service.logIn('username', 'password');
        });

        it('should update the current user', () => {
            expect(service.currentUser?.username).toBe('username');
            expect(service.currentUser?.sessionToken).toBe('token');
        });

        it('should call the logIn API endpoint with the correct parameters', () => {
            expect(logInSpy).toHaveBeenCalledOnceWith({ username: 'username', password: 'password' });
        });

        it('should emit the new user', () => {
            expect(emittedUsers).toEqual([service.currentUser]);
        });
    });

    describe('signing up', () => {
        let signUpSpy: jasmine.Spy<(model: SignupModel) => Promise<SessionViewModel>>;

        beforeEach(async () => {
            signUpSpy = spyOn(restApiService, 'signUp').and.resolveTo({
                token: 'token',
            } as SessionViewModel);
            instantiate();
            await service.signUp('username', 'password');
        });

        it('should update the current user', () => {
            expect(service.currentUser?.username).toBe('username');
            expect(service.currentUser?.sessionToken).toBe('token');
        });

        it('should call the signUp API endpoint with the correct parameters', () => {
            expect(signUpSpy).toHaveBeenCalledOnceWith({ username: 'username', password: 'password' });
        });

        it('should emit the new user', () => {
            expect(emittedUsers).toEqual([service.currentUser]);
        });
    });

    describe('logging out', () => {
        let logOutSpy: jasmine.Spy<() => Promise<void>>;

        beforeEach(async () => {
            logOutSpy = spyOn(restApiService, 'logOut').and.resolveTo();
            instantiate();
            await service.logOut();
        });

        it('should update the current user', () => {
            expect(service.currentUser).toBeUndefined();
        });

        it('should call the logOut API endpoint with the correct parameters', () => {
            expect(logOutSpy).toHaveBeenCalledOnceWith();
        });

        it('should emit the new absent user', () => {
            expect(emittedUsers).toEqual([undefined]);
        });
    });
});
