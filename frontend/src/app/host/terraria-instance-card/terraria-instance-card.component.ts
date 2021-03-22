import { Component, Input } from '@angular/core';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { HostEntity } from 'src/generated/backend';
import { CreateTerrariaInstanceDialogService } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service';

@Component({
    selector: 'tsw-terraria-instance-card',
    templateUrl: './terraria-instance-card.component.html',
    styleUrls: ['./terraria-instance-card.component.sass'],
})
export class TerrariaInstanceCardComponent {
    @Input()
    newInstance?: boolean;

    @Input()
    host?: HostEntity;

    constructor(
        private readonly createDialogService: CreateTerrariaInstanceDialogService,
        private readonly authService: AuthenticationService
    ) {}

    get createDisabledReason(): string | undefined {
        if (!this.host) {
            return 'terraria-instance.card.new.disabled-tooltips.no-host';
        }
        if (!this.authService.currentUser) {
            return 'terraria-instance.card.new.disabled-tooltips.not-logged-in';
        }
        if (!this.authService.canManageHosts) {
            return 'terraria-instance.card.new.disabled-tooltips.no-manage-hosts-permission';
        }
        return undefined;
    }

    onCreateClicked(): void {
        if (!this.host) {
            throw new Error('The host is not defined. Cannot create a Terraria instance.');
        }
        this.createDialogService.openDialog(this.host);
    }
}
