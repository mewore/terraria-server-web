import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { RunServerDialogComponent, RunServerDialogInput, RunServerDialogOutput } from './run-server-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class RunServerDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(data: RunServerDialogInput): Promise<RunServerDialogOutput | undefined> {
        return this.dialog
            .open<RunServerDialogComponent, RunServerDialogInput, RunServerDialogOutput>(RunServerDialogComponent, {
                data,
                maxWidth: '40em',
                minWidth: '20em',
                width: '80%',
            })
            .afterClosed()
            .toPromise();
    }
}
