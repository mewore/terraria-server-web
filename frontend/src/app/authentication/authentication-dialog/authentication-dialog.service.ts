import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AuthenticationDialogComponent, AuthenticationDialogComponentOutput } from './authentication-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class AuthenticationDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(): Promise<AuthenticationDialogComponentOutput | undefined> {
        return this.dialog.open(AuthenticationDialogComponent, {}).afterClosed().toPromise();
    }
}
