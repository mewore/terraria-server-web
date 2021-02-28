import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AuthenticatedUser } from 'src/app/core/types';
import { AuthenticationDialogComponent } from './authentication-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class AuthenticationDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(): Promise<AuthenticatedUser | undefined> {
        return this.dialog.open(AuthenticationDialogComponent, {}).afterClosed().toPromise();
    }
}
