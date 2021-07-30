import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { LogOutDialogComponent, LogOutDialogComponentOutput } from './log-out-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class LogOutDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(): Promise<LogOutDialogComponentOutput> {
        return this.dialog.open(LogOutDialogComponent, {}).afterClosed().toPromise();
    }
}
