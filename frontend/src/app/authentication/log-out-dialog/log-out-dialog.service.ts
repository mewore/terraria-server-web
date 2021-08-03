import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { LogOutDialogComponent, LogOutDialogComponentOutput } from './log-out-dialog.component';

export abstract class LogOutDialogService {
    abstract openDialog(): Promise<LogOutDialogComponentOutput>;
}

@Injectable({
    providedIn: 'root',
})
export class LogOutDialogServiceImpl implements LogOutDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(): Promise<LogOutDialogComponentOutput> {
        return this.dialog.open(LogOutDialogComponent, {}).afterClosed().toPromise();
    }
}
