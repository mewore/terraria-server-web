import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthenticationDialogService } from 'src/app/authentication/authentication-dialog/authentication-dialog.service';
import { LogOutDialogService } from 'src/app/authentication/log-out-dialog/log-out-dialog.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticatedUser } from 'src/app/core/types';

@Component({
    selector: 'tsw-session-info',
    templateUrl: './session-info.component.html',
    styleUrls: ['./session-info.component.sass'],
})
export class SessionInfoComponent implements OnDestroy {
    username?: string;

    private readonly subscription: Subscription;

    constructor(
        private readonly authenticationService: AuthenticationService,
        private readonly authenticationDialogService: AuthenticationDialogService,
        private readonly logOutDialogService: LogOutDialogService
    ) {
        this.subscription = this.authenticationService.userObservable.subscribe({
            next: (newUser: AuthenticatedUser | undefined) => {
                this.username = newUser?.username;
            },
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    onAuthButtonClicked(): void {
        if (this.authenticationService.currentUser) {
            this.logOutDialogService.openDialog();
        } else {
            this.authenticationDialogService.openDialog();
        }
    }
}
