import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ErrorService } from 'src/app/core/services/error.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { TerrariaInstanceEntity, TerrariaInstanceState } from 'src/generated/backend';

export abstract class TerrariaInstanceService {
    abstract isActive(instance: TerrariaInstanceEntity): boolean;
    abstract canDelete(instance: TerrariaInstanceEntity | undefined): boolean;
    abstract delete(instance: TerrariaInstanceEntity | undefined): Promise<TerrariaInstanceEntity | undefined>;
    abstract getStatusLabel(instance: TerrariaInstanceEntity | undefined, deleted: boolean): string;
    abstract isStateBad(instance: TerrariaInstanceEntity | undefined, deleted: boolean): boolean;
}

@Injectable({
    providedIn: 'root',
})
export class TerrariaInstanceServiceImpl implements TerrariaInstanceService {
    private readonly ACTIVE_STATES = new Set<TerrariaInstanceState>([
        'BOOTING_UP',
        'WORLD_MENU',
        'MOD_MENU',
        'CHANGING_MOD_STATE',
        'MOD_BROWSER',
        'WORLD_SIZE_PROMPT',
        'WORLD_DIFFICULTY_PROMPT',
        'WORLD_NAME_PROMPT',
        'MAX_PLAYERS_PROMPT',
        'PORT_PROMPT',
        'AUTOMATICALLY_FORWARD_PORT_PROMPT',
        'PASSWORD_PROMPT',
        'RUNNING',
    ]);

    private readonly BAD_STATES = new Set<TerrariaInstanceState>(['PORT_CONFLICT', 'BROKEN', 'INVALID']);

    constructor(
        private readonly restApi: RestApiService,
        private readonly simpleDialogService: SimpleDialogService,
        private readonly translateService: TranslateService,
        private readonly errorService: ErrorService
    ) {}

    isActive(instance: TerrariaInstanceEntity): boolean {
        return this.ACTIVE_STATES.has(instance.state);
    }

    canDelete(instance: TerrariaInstanceEntity | undefined): boolean {
        return !!instance && !this.isActive(instance);
    }

    async delete(instance: TerrariaInstanceEntity | undefined): Promise<TerrariaInstanceEntity | undefined> {
        if (!instance || this.isActive(instance)) {
            this.errorService.showError(
                'Cannot delete the instance because it is in an invalid state: ' + instance?.state
            );
            return undefined;
        }
        return await this.simpleDialogService.openDialog<TerrariaInstanceEntity>({
            titleKey: 'terraria.instance.dialog.delete.title',
            descriptionKey: 'terraria.instance.dialog.delete.description',
            primaryButton: {
                labelKey: 'terraria.instance.dialog.delete.confirm',
                onClicked: () => this.restApi.updateInstance(instance.id, { newAction: 'DELETE' }),
            },
            warn: true,
        });
    }

    getStatusLabel(instance: TerrariaInstanceEntity | undefined, deleted: boolean): string {
        if (deleted) {
            return this.translateService.instant('terraria.instance.states.deleted');
        }
        if (!instance) {
            return '';
        }
        if (instance.currentAction) {
            return this.translateService.instant('terraria.instance.actions.current.' + instance.currentAction);
        }
        if (instance.pendingAction) {
            return this.translateService.instant('terraria.instance.actions.pending.' + instance.pendingAction);
        }
        return this.translateService.instant('terraria.instance.states.' + instance.state);
    }

    isStateBad(instance: TerrariaInstanceEntity | undefined, deleted: boolean): boolean {
        return deleted || !instance || this.BAD_STATES.has(instance.state);
    }
}
