import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { AuthenticatedUser } from 'src/app/core/types';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { MatDialogStub } from 'src/stubs/mat-dialog.stub';
import { AuthenticationDialogComponent } from './authentication-dialog.component';

import { AuthenticationDialogService, AuthenticationDialogServiceImpl } from './authentication-dialog.service';

describe('AuthenticationDialogService', () => {
    let service: AuthenticationDialogService;

    let dialog: MatDialog;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useClass: MatDialogStub }] });
        dialog = TestBed.inject(MatDialog);

        service = TestBed.inject(AuthenticationDialogServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('openDialog', () => {
        let dialogCloseValue: AuthenticatedUser;
        let openSpy: jasmine.Spy;

        beforeEach(() => {
            dialogCloseValue = {
                authData: 'authData',
                sessionToken: 'sessionToken',
                username: 'username',
            };
            openSpy = spyOn(dialog, 'open').and.returnValue(MatDialogRefStub.withCloseValue(dialogCloseValue));
        });

        it('should be called with the correct parameters', async () => {
            await service.openDialog();
            expect(openSpy).toHaveBeenCalledWith(AuthenticationDialogComponent, {});
        });

        it('should return the correct value', async () => {
            expect(await service.openDialog()).toBe(dialogCloseValue);
        });
    });
});
