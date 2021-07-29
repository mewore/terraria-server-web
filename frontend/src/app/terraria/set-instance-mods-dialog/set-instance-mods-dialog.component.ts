import { AfterViewInit, Component, Inject, ViewChild } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSelectionList } from '@angular/material/list';
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
    tModLoaderVersions: TModLoaderVersionViewModel[] = [];

    loading = false;
    setting = false;

    readonly modOptions: ModOption[] = [];

    @ViewChild(MatSelectionList)
    list?: MatSelectionList;

    constructor(
        private readonly dialog: MatDialogRef<SetInstanceModsDialogComponent, SetInstanceModsDialogOutput>,
        private readonly restApi: RestApiService,
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

    modSelected(mod: ModOption): void {
        mod.selected = true;
    }

    modUnselected(mod: ModOption): void {
        mod.selected = false;
    }

    async onSetClicked(): Promise<void> {
        const selectedMods = this.list?.selectedOptions.selected.map((option) => option.value);
        if (!selectedMods) {
            throw new Error('The selected mods are unknown!');
        }

        this.setting = true;
        try {
            const newInstance = await this.restApi.setInstanceMods(this.instance.id, selectedMods);
            this.dialog.close(newInstance);
        } finally {
            this.setting = false;
        }
    }
}
