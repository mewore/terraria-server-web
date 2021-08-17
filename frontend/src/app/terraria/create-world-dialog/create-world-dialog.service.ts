import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import {
    CreateWorldDialogInput,
    CreateWorldDialogOutput,
    CreateWorldDialogComponent,
} from './create-world-dialog.component';

export abstract class CreateWorldDialogService {
    abstract openDialog(data: CreateWorldDialogInput): Promise<CreateWorldDialogOutput | undefined>;
}

@Injectable({
    providedIn: 'root',
})
export class CreateWorldDialogServiceImpl implements CreateWorldDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(data: CreateWorldDialogInput): Promise<CreateWorldDialogOutput | undefined> {
        return this.dialog
            .open<CreateWorldDialogComponent, CreateWorldDialogInput, CreateWorldDialogOutput>(
                CreateWorldDialogComponent,
                {
                    data,
                    maxWidth: '40em',
                    minWidth: '20em',
                    width: '80%',
                }
            )
            .afterClosed()
            .toPromise();
    }
}
