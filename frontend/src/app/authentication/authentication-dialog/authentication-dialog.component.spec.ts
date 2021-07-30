import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { AuthenticationDialogComponent, AuthenticationDialogComponentOutput } from './authentication-dialog.component';

describe('AuthenticationDialogComponent', () => {
    let component: AuthenticationDialogComponent;
    let fixture: ComponentFixture<AuthenticationDialogComponent>;

    let dialogRef: MatDialogRef<AuthenticationDialogComponent, AuthenticationDialogComponentOutput>;
    let authenticationService: AuthenticationService;

    async function instantiate(): Promise<AuthenticationDialogComponent> {
        fixture = TestBed.createComponent(AuthenticationDialogComponent);
        component = fixture.componentInstance;
        await fixture.whenStable();
        return component;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatDialogModule, ReactiveFormsModule, MatInputModule, MatProgressBarModule],
            declarations: [AuthenticationDialogComponent, TranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);
        authenticationService = TestBed.inject(AuthenticationService);
        await instantiate();
    });

    it('should create', async () => {
        expect(component).toBeTruthy();
    });

    it('should not be busy', async () => {
        expect(component.busy).toBeFalse();
    });

    describe('the inputs', () => {
        it('should be invalid', () => {
            expect(component.valid).toBeFalse();
        });

        describe('when data is entered in them', () => {
            it('should be valid', () => {
                component.usernameFormControl.setValue('username');
                component.passwordFormControl.setValue('password');
                expect(component.valid).toBeTrue();
            });
        });
    });

    describe('when logging in', () => {
        let logInSpy: jasmine.Spy;

        beforeEach(async () => {
            logInSpy = spyOn(authenticationService, 'logIn');
            component.usernameFormControl.setValue('username');
            component.passwordFormControl.setValue('password');
        });

        describe('when the inputs are unset', () => {
            beforeEach(async () => {
                component.usernameFormControl.setValue('');
                component.passwordFormControl.setValue('');
                await component.logIn();
            });

            it('should not try to log in', () => {
                expect(logInSpy).not.toHaveBeenCalled();
            });
        });

        describe('when the login is successful', () => {
            let busyWhileLoggingIn: boolean;
            const userAuth: AuthenticatedUser = {
                authData: 'authData',
                sessionToken: 'sessionToken',
                username: 'username',
            };
            let dialogCloseSpy: jasmine.Spy;

            beforeEach(async () => {
                logInSpy = logInSpy.and.callFake(() => {
                    busyWhileLoggingIn = component.busy;
                    return userAuth;
                });
                dialogCloseSpy = spyOn(dialogRef, 'close');
                await component.logIn();
            });

            describe('during the login itself', () => {
                it('should be busy', () => {
                    expect(busyWhileLoggingIn).toBeTrue();
                });
            });

            it('should have attempted to log in with the correct parameters', () => {
                expect(logInSpy).toHaveBeenCalledWith('username', 'password');
            });

            it('should close with the user authentication as a result', () => {
                expect(dialogCloseSpy).toHaveBeenCalledWith(userAuth);
                expect(logInSpy).toHaveBeenCalledWith('username', 'password');
            });

            it('should not be busy', () => {
                expect(component.busy).toBeFalse();
            });
        });

        describe('when the login fails with status code [400]', () => {
            beforeEach(async () => {
                logInSpy = logInSpy.and.throwError(new HttpErrorResponse({ status: 400 }));
                await component.logIn();
            });

            it('should set the error message key', () => {
                expect(component.errorMessageKey).toEqual('authentication.dialog.errors.bad-request');
            });

            it('should not be busy', () => {
                expect(component.busy).toBeFalse();
            });
        });

        describe('when the login fails with status code [401]', () => {
            beforeEach(async () => {
                logInSpy = logInSpy.and.throwError(new HttpErrorResponse({ status: 401 }));
                await component.logIn();
            });

            it('should set the error message key', () => {
                expect(component.errorMessageKey).toEqual('authentication.dialog.errors.unauthorized');
            });

            it('should not be busy', () => {
                expect(component.busy).toBeFalse();
            });
        });

        describe('when the login fails with an undefined status code', () => {
            let error: Error;

            beforeEach(async () => {
                logInSpy = logInSpy.and.throwError((error = new HttpErrorResponse({})));
            });

            it('should throw the error', async () => {
                expect(await component.logIn().catch((e) => e)).toBe(error);
            });

            it('should not be busy', () => {
                expect(component.busy).toBeFalse();
            });
        });

        describe('when the login fails with a non-HTTP error', () => {
            let error: Error;

            beforeEach(async () => {
                logInSpy = logInSpy.and.throwError((error = new Error()));
            });

            it('should throw the error', async () => {
                expect(await component.logIn().catch((e) => e)).toBe(error);
            });

            it('should not be busy', () => {
                expect(component.busy).toBeFalse();
            });
        });
    });

    // Signing up reuses the same code as logging in
    describe('when singing up', () => {
        let signUpSpy: jasmine.Spy;

        beforeEach(async () => {
            signUpSpy = spyOn(authenticationService, 'signUp');
            component.usernameFormControl.setValue('username');
            component.passwordFormControl.setValue('password');
        });

        describe('when the signup is successful', () => {
            let busyWhileSigningUp: boolean;
            const userAuth: AuthenticatedUser = {
                authData: 'authData',
                sessionToken: 'sessionToken',
                username: 'username',
            };
            let dialogCloseSpy: jasmine.Spy;

            beforeEach(async () => {
                signUpSpy = signUpSpy.and.callFake(() => {
                    busyWhileSigningUp = component.busy;
                    return userAuth;
                });
                dialogCloseSpy = spyOn(dialogRef, 'close');
                await component.signUp();
            });

            describe('during the signup itself', () => {
                it('should be busy', () => {
                    expect(busyWhileSigningUp).toBeTrue();
                });
            });

            it('should have attempted to sign up with the correct parameters', () => {
                expect(signUpSpy).toHaveBeenCalledWith('username', 'password');
            });

            it('should close with the user authentication as a result', () => {
                expect(dialogCloseSpy).toHaveBeenCalledWith(userAuth);
                expect(signUpSpy).toHaveBeenCalledWith('username', 'password');
            });

            it('should not be busy', () => {
                expect(component.busy).toBeFalse();
            });
        });
    });
});
