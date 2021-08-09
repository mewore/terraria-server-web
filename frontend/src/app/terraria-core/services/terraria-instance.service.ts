import { Injectable } from '@angular/core';
import { ErrorService } from 'src/app/core/services/error.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { TerrariaInstanceEntity, TerrariaInstanceState } from 'src/generated/backend';

export abstract class TerrariaInstanceService {
    abstract isRunning(instance: TerrariaInstanceEntity): boolean;
    abstract canDelete(instance: TerrariaInstanceEntity | undefined): boolean;
    abstract delete(instance: TerrariaInstanceEntity | undefined): Promise<TerrariaInstanceEntity | undefined>;
}

@Injectable({
    providedIn: 'root',
})
export class TerrariaInstanceServiceImpl implements TerrariaInstanceService {
    private readonly RUNNING_STATES = new Set<TerrariaInstanceState>([
        'BOOTING_UP',
        'WORLD_MENU',
        'MOD_MENU',
        'MOD_BROWSER',
        'CHANGING_MOD_STATE',
        'MAX_PLAYERS_PROMPT',
        'PORT_PROMPT',
        'AUTOMATICALLY_FORWARD_PORT_PROMPT',
        'PASSWORD_PROMPT',
        'RUNNING',
    ]);

    constructor(
        private readonly restApi: RestApiService,
        private readonly simpleDialogService: SimpleDialogService,
        private readonly errorService: ErrorService
    ) {}

    isRunning(instance: TerrariaInstanceEntity): boolean {
        return this.RUNNING_STATES.has(instance.state);
    }

    canDelete(instance: TerrariaInstanceEntity | undefined): boolean {
        return !!instance && !this.isRunning(instance);
    }

    async delete(instance: TerrariaInstanceEntity | undefined): Promise<TerrariaInstanceEntity | undefined> {
        if (!instance || this.isRunning(instance)) {
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
}
