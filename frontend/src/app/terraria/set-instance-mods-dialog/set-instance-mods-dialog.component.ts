import { Component, Inject, ViewChild } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSelectionList } from '@angular/material/list';
import { ErrorService } from 'src/app/core/services/error.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TerrariaInstanceEntity, TModLoaderVersionViewModel } from 'src/generated/backend';

export type SetInstanceModsDialogInput = TerrariaInstanceEntity;

export type SetInstanceModsDialogOutput = TerrariaInstanceEntity;

interface ModOption {
    name: string;
    selected: boolean;
}

@Component({
    selector: 'tsw-set-instance-mods-dialog',
    templateUrl: './set-instance-mods-dialog.component.html',
    styleUrls: ['./set-instance-mods-dialog.component.sass'],
})
export class SetInstanceModsDialogComponent {
    @ViewChild(MatSelectionList)
    list?: MatSelectionList;

    tModLoaderVersions: TModLoaderVersionViewModel[] = [];

    loading = false;

    readonly modOptions: ModOption[] = [];

    constructor(
        private readonly dialog: MatDialogRef<SetInstanceModsDialogComponent, SetInstanceModsDialogOutput>,
        private readonly restApi: RestApiService,
        private readonly errorService: ErrorService,
        @Inject(MAT_DIALOG_DATA) public instance: SetInstanceModsDialogInput
    ) {
        for (const label of Object.values(this.instance.options)) {
            const matches = label.match(/^(?<modName>.+) \((?<status>enabled|disabled)\)$/)?.groups as {
                modName: string;
                status: string;
            };
            if (!matches) {
                continue;
            }
            this.modOptions.push({
                name: matches.modName,
                selected: matches.status === 'enabled',
            });
        }
    }

    async onSetClicked(): Promise<void> {
        const selectedMods = this.list?.selectedOptions.selected.map((option) => option.value);
        if (!selectedMods) {
            this.errorService.showError(new Error('The selected mods are unknown!'));
            return;
        }

        this.loading = true;
        try {
            const newInstance = await this.restApi.setInstanceMods(this.instance.id, selectedMods);
            this.dialog.close(newInstance);
        } finally {
            this.loading = false;
        }
    }
}
