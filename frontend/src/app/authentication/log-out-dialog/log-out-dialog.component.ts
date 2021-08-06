import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErrorService } from 'src/app/core/services/error.service';

export type LogOutDialogComponentOutput = void;

@Component({
    selector: 'tsw-log-out-dialog',
    templateUrl: './log-out-dialog.component.html',
    styleUrls: ['./log-out-dialog.component.sass'],
})
export class LogOutDialogComponent {
    loading = false;

    constructor(
        private readonly dialog: MatDialogRef<LogOutDialogComponent, LogOutDialogComponentOutput>,
        private readonly authenticationService: AuthenticationService,
        private readonly errorService: ErrorService
    ) {}

    async logOut(): Promise<void> {
        try {
            this.loading = true;
            await this.authenticationService.logOut();
            this.dialog.close();
        } catch (error) {
            if (error instanceof HttpErrorResponse && error.status === 401) {
                this.dialog.close();
            } else {
                this.errorService.showError(error);
            }
        } finally {
            this.loading = false;
        }
    }
}
