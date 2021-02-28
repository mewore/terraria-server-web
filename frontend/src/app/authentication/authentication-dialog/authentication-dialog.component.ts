import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticatedUser } from 'src/app/core/types';

interface AuthenticationSpec {
    username: string;
    password: string;
}

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
        private readonly dialog: MatDialogRef<AuthenticationDialogComponent, AuthenticatedUser>,
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
            (authenticationSpec) =>
                this.authenticationService.logIn(authenticationSpec.username, authenticationSpec.password),
            () => (this.loggingIn = false)
        );
    }

    signUp(): Promise<void> {
        return this.attemptRequest(
            () => (this.signingUp = true),
            (authenticationSpec) =>
                this.authenticationService.signUp(authenticationSpec.username, authenticationSpec.password),
            () => (this.signingUp = false)
        );
    }

    async attemptRequest(
        before: () => void,
        requestFunction: (authenticationSpec: AuthenticationSpec) => Promise<AuthenticatedUser>,
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
            const userAuth = await requestFunction(this.getAuthenticationSpec());
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

    getAuthenticationSpec(): AuthenticationSpec {
        return {
            username: this.usernameFormControl.value,
            password: this.passwordFormControl.value,
        };
    }
}
