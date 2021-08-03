import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AuthenticationDialogComponent, AuthenticationDialogComponentOutput } from './authentication-dialog.component';

export abstract class AuthenticationDialogService {
    abstract openDialog(): Promise<AuthenticationDialogComponentOutput | undefined>;
}

@Injectable({
    providedIn: 'root',
})
export class AuthenticationDialogServiceImpl implements AuthenticationDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(): Promise<AuthenticationDialogComponentOutput | undefined> {
        return this.dialog.open(AuthenticationDialogComponent, {}).afterClosed().toPromise();
    }
}
