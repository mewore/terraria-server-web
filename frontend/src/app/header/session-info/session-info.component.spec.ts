import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { AuthenticationDialogService } from 'src/app/authentication/authentication-dialog/authentication-dialog.service';
import { AuthenticationDialogServiceStub } from 'src/app/authentication/authentication-dialog/authentication-dialog.service.stub';
import { LogOutDialogService } from 'src/app/authentication/log-out-dialog/log-out-dialog.service';
import { LogOutDialogServiceStub } from 'src/app/authentication/log-out-dialog/log-out-dialog.service.stub';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';

import { SessionInfoComponent } from './session-info.component';

describe('SessionInfoComponent', () => {
    let fixture: ComponentFixture<SessionInfoComponent>;
    let component: SessionInfoComponent;

    let authenticationService: AuthenticationService;
    let authenticationDialogService: AuthenticationDialogService;
    let logOutDialogService: LogOutDialogService;
    let userSubject: Subject<AuthenticatedUser>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SessionInfoComponent],
            providers: [
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: AuthenticationDialogService, useClass: AuthenticationDialogServiceStub },
                { provide: LogOutDialogService, useClass: LogOutDialogServiceStub },
            ],
        }).compileComponents();

        authenticationService = TestBed.inject(AuthenticationService);
        userSubject = new Subject<AuthenticatedUser>();
        spyOnProperty(authenticationService, 'userObservable', 'get').and.returnValue(userSubject.asObservable());

        authenticationDialogService = TestBed.inject(AuthenticationDialogService);
        logOutDialogService = TestBed.inject(LogOutDialogService);

        fixture = TestBed.createComponent(SessionInfoComponent);
        await fixture.whenStable();
        component = fixture.componentInstance;
    });

    afterEach(() => {
        fixture.destroy();
    });

    it('should create', async () => {
        expect(component).toBeTruthy();
    });

    describe('the username', () => {
        it('should be undefined', () => {
            expect(component.username).toBeUndefined();
        });

        describe('when there is a new user notification', () => {
            beforeEach(() => {
                userSubject.next({ username: 'username' } as AuthenticatedUser);
            });

            it('should be set to the new username', () => {
                expect(component.username).toBe('username');
            });

            describe('when there is a user notification for the absence of a user', () => {
                beforeEach(() => {
                    userSubject.next(undefined);
                });

                it('should erase the username', () => {
                    expect(component.username).toBeUndefined();
                });
            });
        });
    });

    describe('isAuthenticated', () => {
        describe('when there is a user', () => {
            beforeEach(() => {
                spyOnProperty(authenticationService, 'currentUser', 'get').and.returnValue({} as AuthenticatedUser);
            });

            it('should be true', () => {
                expect(component.isAuthenticated).toBeTrue();
            });
        });

        describe('when there is no user', () => {
            beforeEach(() => {
                spyOnProperty(authenticationService, 'currentUser', 'get').and.returnValue(undefined);
            });

            it('should be true', () => {
                expect(component.isAuthenticated).toBeFalse();
            });
        });
    });

    describe('when Log In is clicked', () => {
        let authenticationDialogSpy: jasmine.Spy<() => void>;

        beforeEach(() => {
            authenticationDialogSpy = spyOn(authenticationDialogService, 'openDialog').and.resolveTo();
            component.onLogInClicked();
        });

        it('should open the authentication dialog', () => {
            expect(authenticationDialogSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('when Log Out is clicked', () => {
        let logOutDialogSpy: jasmine.Spy<() => void>;

        beforeEach(() => {
            logOutDialogSpy = spyOn(logOutDialogService, 'openDialog').and.resolveTo();
            component.onLogOutClicked();
        });

        it('should open the log out dialog', () => {
            expect(logOutDialogSpy).toHaveBeenCalledTimes(1);
        });
    });
});
