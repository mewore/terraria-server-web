import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { MatDialogStub } from 'src/stubs/mat-dialog.stub';
import { SimpleDialogComponent, SimpleDialogInput } from './simple-dialog.component';

import { SimpleDialogService, SimpleDialogServiceImpl } from './simple-dialog.service';

type DialogReturnType = string;

describe('SimpleDialogService', () => {
    let service: SimpleDialogService;

    let dialog: MatDialog;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useClass: MatDialogStub }] });
        dialog = TestBed.inject(MatDialog);

        service = TestBed.inject(SimpleDialogServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('openDialog', () => {
        let inputData: SimpleDialogInput<DialogReturnType>;

        let dialogCloseValue: DialogReturnType;
        let openSpy: jasmine.Spy;

        beforeEach(() => {
            inputData = {
                titleKey: 'title',
            } as SimpleDialogInput<DialogReturnType>;
            dialogCloseValue = 'closeValue';
            openSpy = spyOn(dialog, 'open').and.returnValue(MatDialogRefStub.withCloseValue(dialogCloseValue));
        });

        it('should be called with the correct parameters', async () => {
            await service.openDialog(inputData);
            expect(openSpy).toHaveBeenCalledWith(SimpleDialogComponent, {
                data: inputData,
                maxWidth: '30em',
                minWidth: '20em',
                width: '80%',
            });
        });

        it('should return the correct value', async () => {
            expect(await service.openDialog(inputData)).toBe(dialogCloseValue);
        });
    });
});
