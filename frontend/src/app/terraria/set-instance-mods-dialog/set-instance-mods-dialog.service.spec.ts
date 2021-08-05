import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TerrariaInstanceEntity } from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { MatDialogStub } from 'src/stubs/mat-dialog.stub';
import { SetInstanceModsDialogComponent, SetInstanceModsDialogInput, SetInstanceModsDialogOutput } from './set-instance-mods-dialog.component';
import { SetInstanceModsDialogService, SetInstanceModsDialogServiceImpl } from './set-instance-mods-dialog.service';

describe('SetInstanceModsDialogService', () => {
    let service: SetInstanceModsDialogService;

    let dialog: MatDialog;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useClass: MatDialogStub }] });
        dialog = TestBed.inject(MatDialog);

        service = TestBed.inject(SetInstanceModsDialogServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('openDialog', () => {
        let dialogCloseValue: TerrariaInstanceEntity;
        let openSpy: jasmine.Spy;
        let inputData: SetInstanceModsDialogInput;
        let result: SetInstanceModsDialogOutput | undefined;

        beforeEach(async () => {
            dialogCloseValue = { id: 8 } as TerrariaInstanceEntity;
            openSpy = spyOn(dialog, 'open').and.returnValue(MatDialogRefStub.withCloseValue(dialogCloseValue));
            inputData = { id: 8 } as TerrariaInstanceEntity;
            result = await service.openDialog(inputData);
        });

        it('should be called with the correct parameters', async () => {
            expect(openSpy).toHaveBeenCalledWith(SetInstanceModsDialogComponent, {
                data: inputData,
                maxWidth: '50em',
                minWidth: '20em',
                width: '80%',
            });
        });

        it('should return the correct value', async () => {
            expect(result).toBe(dialogCloseValue);
        });
    });
});
