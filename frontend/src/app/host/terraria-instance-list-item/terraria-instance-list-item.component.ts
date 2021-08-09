import { Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErrorService } from 'src/app/core/services/error.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TerrariaInstanceService } from 'src/app/terraria-core/services/terraria-instance.service';
import { HostEntity, TerrariaInstanceAction, TerrariaInstanceEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-instance-list-item',
    templateUrl: './terraria-instance-list-item.component.html',
    styleUrls: ['./terraria-instance-list-item.component.sass'],
})
export class TerrariaInstanceListItemComponent implements OnDestroy {
    @Input()
    host?: HostEntity;

    @Input()
    instance?: TerrariaInstanceEntity;

    @ViewChild('nameInputElement')
    nameInputElement?: ElementRef<HTMLInputElement>;

    readonly nameInput = new FormControl('', [(_control) => (this.canManageTerraria ? null : { permission: true })]);

    action?: TerrariaInstanceAction | 'RENAME';

    loading = false;

    private readonly userSubscription: Subscription;

    constructor(
        private readonly authenticationService: AuthenticationService,
        private readonly restApi: RestApiService,
        private readonly errorService: ErrorService,
        private readonly terrariaInstanceService: TerrariaInstanceService
    ) {
        this.userSubscription = authenticationService.userObservable.subscribe({
            next: () => this.nameInput.updateValueAndValidity(),
        });
    }

    ngOnDestroy(): void {
        this.userSubscription.unsubscribe();
    }

    get canManageTerraria(): boolean {
        return this.authenticationService.canManageTerraria;
    }

    get canDelete(): boolean {
        return !this.action && !this.instance?.pendingAction && this.terrariaInstanceService.canDelete(this.instance);
    }

    get renaming(): boolean {
        return this.action === 'RENAME';
    }

    onRenameClicked(): void {
        const instance = this.instance;
        if (!instance || !this.canManageTerraria) {
            return;
        }
        this.action = 'RENAME';
        this.nameInput.setValue(instance.name);
        this.nameInput.enable();
        setTimeout(() => this.focusOnNameInput());
    }

    focusOnNameInput(): void {
        if (this.nameInputElement) {
            this.nameInputElement.nativeElement.focus();
        }
    }

    onRenameCancelled(): void {
        this.action = undefined;
    }

    onRenameConfirmed(event: Event): void {
        if (this.loading || !this.renaming) {
            return;
        }
        event.stopPropagation();
        event.preventDefault();
        this.renameInstance();
    }

    async renameInstance(): Promise<void> {
        const newName = this.nameInput.value;
        if (!newName || newName === this.instance?.name) {
            this.action = undefined;
            return;
        }
        if (!this.canManageTerraria) {
            this.nameInput.updateValueAndValidity();
            return;
        }
        this.nameInput.disable();
        this.performInstanceAction((instance) => this.restApi.updateInstance(instance.id, { newName }));
    }

    onDeleteClicked(): void {
        this.action = 'DELETE';
        this.performInstanceAction((instance) => this.terrariaInstanceService.delete(instance));
    }

    private async performInstanceAction(
        action: (instance: TerrariaInstanceEntity) => Promise<TerrariaInstanceEntity | undefined>
    ): Promise<void> {
        if (this.loading) {
            return this.errorService.showError('Already loading!');
        }

        const instance = this.instance;
        if (!instance) {
            return this.errorService.showError('The instance is not defined!');
        }

        this.loading = true;
        try {
            this.instance = (await action(instance)) ?? this.instance;
        } finally {
            this.loading = false;
            this.action = undefined;
        }
    }
}
