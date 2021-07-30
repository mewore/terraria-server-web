import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { MatDialogStub } from 'src/stubs/mat-dialog.stub';
import { LogOutDialogComponent } from './log-out-dialog.component';
import { LogOutDialogService } from './log-out-dialog.service';

describe('LogOutDialogService', () => {
    let service: LogOutDialogService;

    let dialog: MatDialog;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useClass: MatDialogStub }] });
        dialog = TestBed.inject(MatDialog);

        service = TestBed.inject(LogOutDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('openDialog', () => {
        let openSpy: jasmine.Spy;

        beforeEach(() => {
            openSpy = spyOn(dialog, 'open').and.returnValue(MatDialogRefStub.withCloseValue(undefined));
        });

        it('should be called with the correct parameters', async () => {
            await service.openDialog();
            expect(openSpy).toHaveBeenCalledWith(LogOutDialogComponent, {});
        });

        it('should return the correct value', async () => {
            expect(await service.openDialog()).toBeUndefined();
        });
    });
});
