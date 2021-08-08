import { Component, ElementRef, Input, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';

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

    renaming = false;

    loading = false;

    private readonly userSubscription: Subscription;

    constructor(
        private readonly authenticationService: AuthenticationService,
        private readonly restApiService: RestApiService
    ) {
        this.userSubscription = authenticationService.userObservable.subscribe({
            next: () => this.nameInput.updateValueAndValidity(),
        });
    }

    ngOnDestroy(): void {
        this.userSubscription.unsubscribe();
    }

    get canManageTerraria() {
        return this.authenticationService.canManageTerraria;
    }

    onRenameClicked(): void {
        const instance = this.instance;
        if (!instance || !this.canManageTerraria) {
            return;
        }
        this.renaming = true;
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
        this.renaming = false;
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
        const instance = this.instance;
        if (!instance) {
            return;
        }
        const newName = this.nameInput.value;
        if (!newName || newName === instance.name) {
            this.renaming = false;
            return;
        }
        if (!this.canManageTerraria) {
            this.nameInput.updateValueAndValidity();
            return;
        }
        try {
            this.nameInput.disable();
            this.loading = true;
            this.instance = await this.restApiService.updateInstance(instance.id, { newName });
        } finally {
            this.loading = false;
            this.renaming = false;
        }
    }
}
