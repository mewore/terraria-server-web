import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo } from 'src/test-util/dialog-info';
import { AuthenticationDialogComponent, AuthenticationDialogComponentOutput } from './authentication-dialog.component';

describe('AuthenticationDialogComponent', () => {
    let fixture: ComponentFixture<AuthenticationDialogComponent>;
    let component: AuthenticationDialogComponent;
    let dialog: DialogInfo;

    let dialogRef: MatDialogRef<AuthenticationDialogComponent, AuthenticationDialogComponentOutput>;
    let authenticationService: AuthenticationService;
    let errorService: ErrorService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatDialogModule, ReactiveFormsModule, MatInputModule, MatProgressBarModule, NoopAnimationsModule],
            declarations: [AuthenticationDialogComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);
        authenticationService = TestBed.inject(AuthenticationService);
        errorService = TestBed.inject(ErrorService);

        [fixture, component] = await initComponent(AuthenticationDialogComponent);
        dialog = new MaterialDialogInfo(fixture);
    });

    it('should have the correct title', async () => {
        expect(dialog.title).toBe('Log in or sign up');
    });

    it('should have the correct buttons', async () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Sign up', 'Log in']);
    });

    it('should not have a loading indicator', async () => {
        expect(dialog.hasLoadingIndicator).toBeFalse();
    });

    describe('while logging in', () => {
        beforeEach(() => {
            component.loggingIn = true;
            fixture.detectChanges();
        });

        it('only the Cancel button should be enabled', async () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        it('have a loading indicator', async () => {
            expect(dialog.hasLoadingIndicator).toBeTrue();
        });
    });

    describe('while signing up', () => {
        beforeEach(() => {
            component.loggingIn = true;
            fixture.detectChanges();
        });

        it('only the Cancel button should be enabled', async () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        it('have a loading indicator', async () => {
            expect(dialog.hasLoadingIndicator).toBeTrue();
        });
    });

    describe('when no data is entered', () => {
        it('only the Cancel button should be enabled', async () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        describe('logIn', () => {
            let logInSpy: jasmine.Spy;

            beforeEach(() => {
                logInSpy = spyOn(authenticationService, 'logIn');
                component.logIn();
            });

            it('should not try to log in', () => {
                expect(logInSpy).not.toHaveBeenCalled();
            });
        });
    });

    describe('when valid data is entered', () => {
        beforeEach(() => {
            component.usernameFormControl.setValue('username');
            component.passwordFormControl.setValue('password');
            fixture.detectChanges();
        });

        it('all buttons should be enabled', async () => {
            expect(dialog.enabledButtons).toEqual(['Cancel', 'Sign up', 'Log in']);
        });

        describe('when "Log in" is clicked', () => {
            let logInSpy: jasmine.Spy;

            beforeEach(() => {
                logInSpy = spyOn(authenticationService, 'logIn');
            });

            describe('when the login is successful', () => {
                let busyWhileLoggingIn: boolean;
                const userAuth: AuthenticatedUser = {
                    authData: 'authData',
                    sessionToken: 'sessionToken',
                    username: 'username',
                };
                let dialogCloseSpy: jasmine.Spy;

                beforeEach(() => {
                    logInSpy = logInSpy.and.callFake(() => {
                        busyWhileLoggingIn = component.loading;
                        return userAuth;
                    });
                    dialogCloseSpy = spyOn(dialogRef, 'close');
                    dialog.clickButton('Log in');
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
                });

                it('should not have a loading indicator', async () => {
                    expect(dialog.hasLoadingIndicator).toBeFalse();
                });
            });

            describe('when the login fails with status code [400]', () => {
                beforeEach(() => {
                    logInSpy = logInSpy.and.throwError(new HttpErrorResponse({ status: 400 }));
                    dialog.clickButton('Log in');
                });

                it('should set the error message key', () => {
                    expect(component.errorMessageKey).toEqual('authentication.dialog.errors.bad-request');
                });

                it('should not have a loading indicator', async () => {
                    expect(dialog.hasLoadingIndicator).toBeFalse();
                });
            });

            describe('when the login fails with status code [401]', () => {
                beforeEach(() => {
                    logInSpy = logInSpy.and.throwError(new HttpErrorResponse({ status: 401 }));
                    dialog.clickButton('Log in');
                });

                it('should set the error message key', () => {
                    expect(component.errorMessageKey).toEqual('authentication.dialog.errors.unauthorized');
                });

                it('should not have a loading indicator', async () => {
                    expect(dialog.hasLoadingIndicator).toBeFalse();
                });
            });

            describe('when the login fails with an undefined status code', () => {
                let error: Error;
                let showErrorSpy: jasmine.Spy<(error: Error) => void>;

                beforeEach(() => {
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    logInSpy = logInSpy.and.throwError((error = new HttpErrorResponse({})));
                    dialog.clickButton('Log in');
                });

                it('should show the error', async () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith(error);
                });

                it('should not have a loading indicator', async () => {
                    expect(dialog.hasLoadingIndicator).toBeFalse();
                });
            });

            describe('when the login fails with a non-HTTP error', () => {
                let error: Error;
                let showErrorSpy: jasmine.Spy<(error: Error) => void>;

                beforeEach(() => {
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    logInSpy = logInSpy.and.throwError((error = new Error()));
                    dialog.clickButton('Log in');
                });

                it('should show the error', async () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith(error);
                });

                it('should not have a loading indicator', async () => {
                    expect(dialog.hasLoadingIndicator).toBeFalse();
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
                fixture.detectChanges();
            });

            describe('when the signup is successful', () => {
                let busyWhileSigningUp: boolean;
                const userAuth: AuthenticatedUser = {
                    authData: 'authData',
                    sessionToken: 'sessionToken',
                    username: 'username',
                };
                let dialogCloseSpy: jasmine.Spy;

                beforeEach(() => {
                    signUpSpy = signUpSpy.and.callFake(() => {
                        busyWhileSigningUp = component.loading;
                        return userAuth;
                    });
                    dialogCloseSpy = spyOn(dialogRef, 'close');
                    dialog.clickButton('Sign up');
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
                });

                it('should not have a loading indicator', async () => {
                    expect(dialog.hasLoadingIndicator).toBeFalse();
                });
            });
        });
    });
});
