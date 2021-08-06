import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo } from 'src/test-util/dialog-info';

import { LogOutDialogComponent, LogOutDialogComponentOutput } from './log-out-dialog.component';

describe('LogOutDialogComponent', () => {
    let component: LogOutDialogComponent;
    let fixture: ComponentFixture<LogOutDialogComponent>;
    let dialog: DialogInfo;

    let dialogRef: MatDialogRef<LogOutDialogComponent, LogOutDialogComponentOutput>;
    let authenticationService: AuthenticationService;
    let errorService: ErrorService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatDialogModule, MatProgressBarModule],
            declarations: [LogOutDialogComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);
        authenticationService = TestBed.inject(AuthenticationService);
        errorService = TestBed.inject(ErrorService);

        [fixture, component] = await initComponent(LogOutDialogComponent);
        dialog = new MaterialDialogInfo(fixture);
    });

    it('should have the correct title', async () => {
        expect(dialog.title).toBe('Log out');
    });

    it('should have the correct buttons', async () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Log out']);
    });

    it('all buttons should be enabled', async () => {
        expect(dialog.enabledButtons).toEqual(['Cancel', 'Log out']);
    });

    it('should not have a loading indicator', async () => {
        expect(dialog.hasLoadingIndicator).toBeFalse();
    });

    describe('while loading', () => {
        beforeEach(() => {
            component.loading = true;
            fixture.detectChanges();
        });

        it('only the Cancel button should be enabled', async () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        it('should have a loading indicator', async () => {
            expect(dialog.hasLoadingIndicator).toBeTrue();
        });
    });

    describe('when logging out', () => {
        let logOutSpy: jasmine.Spy<() => Promise<void>>;
        let dialogCloseSpy: jasmine.Spy<() => void>;

        beforeEach(() => {
            logOutSpy = spyOn(authenticationService, 'logOut');
            dialogCloseSpy = spyOn(dialogRef, 'close');
        });

        describe('when the logout is successful', () => {
            let loadingWhileLoggingOut: boolean;

            beforeEach(() => {
                logOutSpy = logOutSpy.and.callFake(async () => {
                    loadingWhileLoggingOut = component.loading;
                });
                dialog.clickButton('Log out');
            });

            describe('during the logout itself', () => {
                it('should be loading', () => {
                    expect(loadingWhileLoggingOut).toBeTrue();
                });
            });

            it('should have attempted to log out with the correct parameters', () => {
                expect(logOutSpy).toHaveBeenCalledWith();
            });

            it('should close the dialog', () => {
                expect(dialogCloseSpy).toHaveBeenCalledWith();
            });

            it('should not have a loading indicator', () => {
                expect(dialog.hasLoadingIndicator).toBeFalse();
            });
        });

        describe('when the logout fails with status code [401]', () => {
            beforeEach(() => {
                logOutSpy = logOutSpy.and.throwError(new HttpErrorResponse({ status: 401 }));
                dialog.clickButton('Log out');
            });

            it('should close the dialog', () => {
                expect(dialogCloseSpy).toHaveBeenCalledOnceWith();
            });

            it('should not have a loading indicator', () => {
                expect(dialog.hasLoadingIndicator).toBeFalse();
            });
        });

        describe('when the logout fails with an undefined status code', () => {
            let showErrorSpy: jasmine.Spy<(error: Error) => void>;
            let error: Error;

            beforeEach(() => {
                logOutSpy = logOutSpy.and.throwError((error = new HttpErrorResponse({})));
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                dialog.clickButton('Log out');
            });

            it('should show the error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(error);
            });

            it('should not have a loading indicator', () => {
                expect(dialog.hasLoadingIndicator).toBeFalse();
            });
        });

        describe('when the logout fails with a non-HTTP error', () => {
            let showErrorSpy: jasmine.Spy<(error: Error) => void>;
            let error: Error;

            beforeEach(() => {
                logOutSpy = logOutSpy.and.throwError((error = new Error()));
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                dialog.clickButton('Log out');
            });

            it('should show the error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(error);
            });

            it('should not have a loading indicator', () => {
                expect(dialog.hasLoadingIndicator).toBeFalse();
            });
        });
    });
});
