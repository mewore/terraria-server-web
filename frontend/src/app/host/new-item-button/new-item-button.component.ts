import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AccountTypeEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-new-item-button',
    templateUrl: './new-item-button.component.html',
    styleUrls: ['./new-item-button.component.sass'],
})
export class NewItemButtonComponent {
    @Input()
    requiredPermissions?: Array<Exclude<keyof AccountTypeEntity, 'id'>>;

    @Output()
    press = new EventEmitter<void>();

    constructor(
        private readonly authService: AuthenticationService,
        private readonly translateService: TranslateService
    ) {}

    get createDisabledReason(): string | undefined {
        if (!this.requiredPermissions) {
            return undefined;
        }
        const accountType: { [key: string]: boolean } = this.authService.currentUser?.accountType || {};
        const lines: string[] = this.requiredPermissions
            .filter((permission) => !accountType[permission])
            .map((permission) => this.translateService.instant('no-permission.' + permission));
        return lines.length > 0 ? lines.join('\n') : undefined;
    }

    clicked(): void {
        if (!this.createDisabledReason) {
            this.press.emit();
        }
    }
}
