import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TerrariaInstanceEntity } from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { MatDialogStub } from 'src/stubs/mat-dialog.stub';
import { RunServerDialogComponent, RunServerDialogInput, RunServerDialogOutput } from './run-server-dialog.component';

import { RunServerDialogService, RunServerDialogServiceImpl } from './run-server-dialog.service';

describe('RunServerDialogService', () => {
    let service: RunServerDialogService;

    let dialog: MatDialog;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useClass: MatDialogStub }] });
        dialog = TestBed.inject(MatDialog);

        service = TestBed.inject(RunServerDialogServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('openDialog', () => {
        let dialogCloseValue: TerrariaInstanceEntity;
        let openSpy: jasmine.Spy;
        let inputData: RunServerDialogInput;
        let result: RunServerDialogOutput | undefined;

        beforeEach(async () => {
            dialogCloseValue = { id: 8 } as TerrariaInstanceEntity;
            openSpy = spyOn(dialog, 'open').and.returnValue(MatDialogRefStub.withCloseValue(dialogCloseValue));
            inputData = { hostId: 1, instance: { id: 8 } as TerrariaInstanceEntity } as RunServerDialogInput;
            result = await service.openDialog(inputData);
        });

        it('should be called with the correct parameters', async () => {
            expect(openSpy).toHaveBeenCalledWith(RunServerDialogComponent, {
                data: inputData,
                maxWidth: '40em',
                minWidth: '20em',
                width: '80%',
            });
        });

        it('should return the correct value', async () => {
            expect(result).toBe(dialogCloseValue);
        });
    });
});
