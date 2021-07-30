import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';

import { LogOutDialogComponent, LogOutDialogComponentOutput } from './log-out-dialog.component';

describe('LogOutDialogComponent', () => {
    let component: LogOutDialogComponent;
    let fixture: ComponentFixture<LogOutDialogComponent>;

    let dialogRef: MatDialogRef<LogOutDialogComponent, LogOutDialogComponentOutput>;
    let authenticationService: AuthenticationService;

    async function instantiate(): Promise<LogOutDialogComponent> {
        fixture = TestBed.createComponent(LogOutDialogComponent);
        await fixture.whenStable();
        return fixture.componentInstance;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatDialogModule, MatProgressBarModule],
            declarations: [LogOutDialogComponent, TranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);
        authenticationService = TestBed.inject(AuthenticationService);
        component = await instantiate();
    });

    it('should create', async () => {
        expect(component).toBeTruthy();
    });

    it('should not be loading', async () => {
        expect(component.loading).toBeFalse();
    });

    describe('when logging out', () => {
        let logOutSpy: jasmine.Spy<() => Promise<void>>;

        beforeEach(() => {
            logOutSpy = spyOn(authenticationService, 'logOut');
        });

        describe('when the logout is successful', () => {
            let loadingWhileLoggingOut: boolean;
            let dialogCloseSpy: jasmine.Spy;

            beforeEach(async () => {
                logOutSpy = logOutSpy.and.callFake(async () => {
                    loadingWhileLoggingOut = component.loading;
                });
                dialogCloseSpy = spyOn(dialogRef, 'close');
                await component.logOut();
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

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });

        describe('when the logout fails with status code [401]', () => {
            let dialogCloseSpy: jasmine.Spy;

            beforeEach(async () => {
                logOutSpy = logOutSpy.and.throwError(new HttpErrorResponse({ status: 401 }));
                dialogCloseSpy = spyOn(dialogRef, 'close');
                await component.logOut();
            });

            it('should close the dialog', () => {
                expect(dialogCloseSpy).toHaveBeenCalledWith();
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });

        describe('when the logout fails with an undefined status code', () => {
            let error: Error;

            beforeEach(async () => {
                logOutSpy = logOutSpy.and.throwError((error = new HttpErrorResponse({})));
            });

            it('should throw the error', async () => {
                expect(await component.logOut().catch((e) => e)).toBe(error);
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });

        describe('when the logout fails with a non-HTTP error', () => {
            let error: Error;

            beforeEach(async () => {
                logOutSpy = logOutSpy.and.throwError((error = new Error()));
            });

            it('should throw the error', async () => {
                expect(await component.logOut().catch((e) => e)).toBe(error);
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });
    });
});
