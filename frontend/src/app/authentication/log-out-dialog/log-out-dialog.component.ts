import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

@Component({
    selector: 'tsw-log-out-dialog',
    templateUrl: './log-out-dialog.component.html',
    styleUrls: ['./log-out-dialog.component.sass'],
})
export class LogOutDialogComponent {
    loggingOut = false;

    constructor(
        private readonly dialog: MatDialogRef<LogOutDialogComponent, void>,
        private readonly authenticationService: AuthenticationService
    ) {}

    async logOut(): Promise<void> {
        this.loggingOut = true;
        try {
            await this.authenticationService.logOut();
            this.dialog.close();
        } catch (error) {
            if (error instanceof HttpErrorResponse) {
                switch (error.status) {
                    case 401:
                        this.dialog.close();
                        break;
                    default:
                        throw error;
                }
            } else {
                throw error;
            }
        } finally {
            this.loggingOut = false;
        }
    }
}
