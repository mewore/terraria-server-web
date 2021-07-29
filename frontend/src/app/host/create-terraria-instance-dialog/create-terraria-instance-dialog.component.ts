import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TswValidators } from 'src/app/core/tsw-validators';
import {
    HostEntity,
    TerrariaInstanceDefinitionModel,
    TerrariaInstanceEntity,
    TModLoaderVersionViewModel,
} from 'src/generated/backend';

export type CreateTerrariaInstanceDialogInput = HostEntity;

export type CreateTerrariaInstanceDialogOutput = TerrariaInstanceEntity;

@Component({
    selector: 'tsw-create-terraria-instance-dialog',
    templateUrl: './create-terraria-instance-dialog.component.html',
    styleUrls: ['./create-terraria-instance-dialog.component.sass'],
})
export class CreateTerrariaInstanceDialogComponent implements OnInit {
    tModLoaderVersions: TModLoaderVersionViewModel[] = [];

    loading = false;
    creating = false;

    readonly instanceNameInput = new FormControl('', [TswValidators.notBlank]);

    readonly terrariaServerArchiveUrlInput = new FormControl('', [
        TswValidators.notBlank,
        TswValidators.fileExtension('zip'),
        TswValidators.terrariaUrl,
    ]);

    readonly modLoaderReleaseInput = new FormControl({ value: undefined, disabled: true }, [Validators.required]);

    readonly form = new FormGroup({
        instanceName: this.instanceNameInput,
        terrariaServerArchiveUrl: this.terrariaServerArchiveUrlInput,
        modLoaderReleaseId: this.modLoaderReleaseInput,
    } as Record<keyof TerrariaInstanceDefinitionModel, FormControl>);

    constructor(
        private readonly dialog: MatDialogRef<
            CreateTerrariaInstanceDialogComponent,
            CreateTerrariaInstanceDialogOutput
        >,
        private readonly restApi: RestApiService,
        @Inject(MAT_DIALOG_DATA) public data: CreateTerrariaInstanceDialogInput
    ) {}

    async ngOnInit(): Promise<void> {
        this.form.markAllAsTouched();
        try {
            this.loading = true;
            this.tModLoaderVersions = await this.restApi.getTModLoaderVersions();
        } finally {
            this.loading = false;
        }
        this.modLoaderReleaseInput.setValue(this.tModLoaderVersions[0].releaseId);
        this.modLoaderReleaseInput.enable();
    }

    async onCreateClicked(): Promise<void> {
        if (this.form.invalid) {
            this.form.markAsTouched();
            return;
        }

        this.creating = true;
        try {
            const newInstance = await this.restApi.createTerrariaInstance(this.data.id, {
                instanceName: this.instanceNameInput.value,
                terrariaServerArchiveUrl: this.terrariaServerArchiveUrlInput.value,
                modLoaderReleaseId: this.modLoaderReleaseInput.value,
            });
            this.dialog.close(newInstance);
        } finally {
            this.creating = false;
        }
    }
}
