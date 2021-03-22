import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TswValidators } from 'src/app/core/tsw-validators';
import { HostEntity, TModLoaderVersionViewModel } from 'src/generated/backend';

export type CreateTerrariaInstanceDialogInput = HostEntity;

export type CreateTerrariaInstanceDialogOutput = void;

@Component({
    selector: 'tsw-create-terraria-instance-dialog',
    templateUrl: './create-terraria-instance-dialog.component.html',
    styleUrls: ['./create-terraria-instance-dialog.component.sass'],
})
export class CreateTerrariaInstanceDialogComponent implements OnInit {
    tModLoaderVersions: TModLoaderVersionViewModel[] = [];

    creating = false;

    readonly terrariaServerUrlFormControl = new FormControl('', [
        Validators.required,
        TswValidators.fileExtension('zip'),
        TswValidators.terrariaUrl,
    ]);

    readonly tModLoaderVersionFormControl = new FormControl(undefined, [Validators.required]);

    readonly form = new FormGroup({
        tModLoaderVersionFormControl: this.tModLoaderVersionFormControl,
        terrariaServerUrlFormControl: this.terrariaServerUrlFormControl,
    });

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
        this.tModLoaderVersions = await this.restApi.getTModLoaderVersions();
        this.tModLoaderVersionFormControl.setValue(this.tModLoaderVersions[0].releaseId);
    }

    async onCreateClicked(): Promise<void> {
        if (this.form.invalid) {
            this.form.markAsTouched();
            return;
        }

        this.creating = true;
        try {
            await this.restApi.createTerrariaInstance({
                hostId: this.data.id,
                terrariaServerArchiveUrl: this.terrariaServerUrlFormControl.value,
                modLoaderReleaseId: this.tModLoaderVersionFormControl.value,
            });
            this.dialog.close();
        } finally {
            this.creating = false;
        }
    }
}
