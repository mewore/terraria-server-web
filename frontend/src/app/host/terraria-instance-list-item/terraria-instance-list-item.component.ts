import { Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErrorService } from 'src/app/core/services/error.service';
import { MessageService } from 'src/app/core/services/message.service';
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
    set instance(newInstance: TerrariaInstanceEntity | undefined) {
        this.privateInstance = newInstance;
        if (newInstance) {
            const instanceId = newInstance.id;
            this.subscriptions.push(
                this.messageService.watchInstanceDeletion(newInstance).subscribe({
                    next: () => (
                        this.deletedInstanceIds.add(instanceId),
                        this.nameInput.updateValueAndValidity(),
                        this.nameInput.markAsTouched()
                    ),
                })
            );
        }
    }

    get instance(): TerrariaInstanceEntity | undefined {
        return this.privateInstance;
    }

    @ViewChild('nameInputElement')
    nameInputElement?: ElementRef<HTMLInputElement>;

    readonly nameInput = new FormControl('', [
        () => (this.canManageTerraria ? null : { permission: true }),
        () => (this.deleted ? { deleted: true } : null),
    ]);

    action?: TerrariaInstanceAction | 'RENAME';

    loading = false;

    private readonly deletedInstanceIds = new Set<number>();

    private readonly subscriptions: Subscription[] = [];

    private privateInstance?: TerrariaInstanceEntity;

    constructor(
        private readonly authenticationService: AuthenticationService,
        private readonly restApi: RestApiService,
        private readonly errorService: ErrorService,
        private readonly messageService: MessageService,
        private readonly terrariaInstanceService: TerrariaInstanceService
    ) {
        this.subscriptions.push(
            authenticationService.userObservable.subscribe({
                next: () => this.nameInput.updateValueAndValidity(),
            })
        );
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    }

    get deleted(): boolean {
        const instance = this.instance;
        return !!instance && this.deletedInstanceIds.has(instance.id);
    }

    get canManageTerraria(): boolean {
        return this.authenticationService.canManageTerraria;
    }

    get canRename(): boolean {
        return this.canManageTerraria && !this.deleted;
    }

    get canDelete(): boolean {
        return (
            !this.action &&
            !this.deleted &&
            !this.instance?.pendingAction &&
            this.canManageTerraria &&
            this.terrariaInstanceService.canDelete(this.instance)
        );
    }

    get renaming(): boolean {
        return this.action === 'RENAME';
    }

    onRenameClicked(): void {
        const instance = this.instance;
        if (!instance || !this.canRename) {
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
        if (!this.canRename) {
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
