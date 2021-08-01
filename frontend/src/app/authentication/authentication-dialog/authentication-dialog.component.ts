import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticatedUser } from 'src/app/core/types';

export type AuthenticationDialogComponentOutput = AuthenticatedUser;

@Component({
    selector: 'tsw-authentication-dialog',
    templateUrl: './authentication-dialog.component.html',
    styleUrls: ['./authentication-dialog.component.sass'],
})
export class AuthenticationDialogComponent {
    signingUp = false;
    loggingIn = false;

    readonly usernameFormControl = new FormControl('', [Validators.required]);
    readonly passwordFormControl = new FormControl('', [Validators.required]);

    errorMessageKey = '';

    constructor(
        private readonly dialog: MatDialogRef<AuthenticationDialogComponent, AuthenticationDialogComponentOutput>,
        private readonly authenticationService: AuthenticationService
    ) {}

    get valid(): boolean {
        return this.usernameFormControl.valid && this.passwordFormControl.valid;
    }

    get busy(): boolean {
        return this.signingUp || this.loggingIn;
    }

    logIn(): Promise<void> {
        return this.attemptRequest(
            () => (this.loggingIn = true),
            () => this.authenticationService.logIn(this.usernameFormControl.value, this.passwordFormControl.value),
            () => (this.loggingIn = false)
        );
    }

    signUp(): Promise<void> {
        return this.attemptRequest(
            () => (this.signingUp = true),
            () => this.authenticationService.signUp(this.usernameFormControl.value, this.passwordFormControl.value),
            () => (this.signingUp = false)
        );
    }

    async attemptRequest(
        before: () => void,
        requestFunction: () => Promise<AuthenticatedUser>,
        after: () => void
    ): Promise<void> {
        this.usernameFormControl.markAsDirty();
        this.passwordFormControl.markAsDirty();
        if (!this.valid) {
            return;
        }
        this.errorMessageKey = '';
        try {
            before();
            const userAuth = await requestFunction();
            this.dialog.close(userAuth);
        } catch (error) {
            if (error instanceof HttpErrorResponse) {
                switch (error.status) {
                    // TODO: Use an error model with an error message key instead
                    case 400:
                        this.errorMessageKey = 'authentication.dialog.errors.bad-request';
                        break;
                    case 401:
                        this.errorMessageKey = 'authentication.dialog.errors.unauthorized';
                        break;
                    default:
                        throw error;
                }
            } else {
                throw error;
            }
        } finally {
            after();
        }
    }
}
