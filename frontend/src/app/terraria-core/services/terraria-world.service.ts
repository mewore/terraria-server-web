import { Injectable } from '@angular/core';
import { ErrorService } from 'src/app/core/services/error.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { TerrariaWorldEntity } from 'src/generated/backend';

export abstract class TerrariaWorldService {
    abstract canDelete(world: TerrariaWorldEntity | undefined, usedWorldIds: Set<number>): boolean;
    abstract delete(world: TerrariaWorldEntity | undefined): Promise<boolean | undefined>;
}

@Injectable({
    providedIn: 'root',
})
export class TerrariaWorldServiceImpl implements TerrariaWorldService {
    constructor(
        private readonly restApi: RestApiService,
        private readonly simpleDialogService: SimpleDialogService,
        private readonly errorService: ErrorService
    ) {}

    canDelete(world: TerrariaWorldEntity | undefined, usedWorldIds: Set<number>): boolean {
        return !!world && !usedWorldIds.has(world.id);
    }

    async delete(world: TerrariaWorldEntity | undefined): Promise<boolean | undefined> {
        if (!world) {
            this.errorService.showError('Cannot delete the world because it is undefined');
            return false;
        }
        return await this.simpleDialogService.openDialog<boolean>({
            titleKey: 'terraria.world.dialog.delete.title',
            descriptionKey: 'terraria.world.dialog.delete.description',
            primaryButton: {
                labelKey: 'terraria.world.dialog.delete.confirm',
                onClicked: () => this.restApi.deleteWorld(world.id).then(() => true),
            },
            warn: true,
        });
    }
}
