import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TswValidators } from 'src/app/core/tsw-validators';
import { TModLoaderVersionViewModel } from 'src/generated/backend';

@Component({
    selector: 'tsw-create-terraria-instance-dialog',
    templateUrl: './create-terraria-instance-dialog.component.html',
    styleUrls: ['./create-terraria-instance-dialog.component.sass'],
})
export class CreateTerrariaInstanceDialogComponent implements OnInit {
    tModLoaderVersions: TModLoaderVersionViewModel[] = [];

    creating = false;

    readonly tModLoaderVersionFormControl = new FormControl({ disabled: true }, [Validators.required]);
    readonly terrariaServerUrlFormControl = new FormControl('', [
        Validators.required,
        TswValidators.fileExtension('zip'),
        TswValidators.terrariaUrl,
    ]);

    readonly form = new FormGroup({
        tModLoaderVersionFormControl: this.tModLoaderVersionFormControl,
        terrariaServerUrlFormControl: this.terrariaServerUrlFormControl,
    });

    constructor(
        private readonly dialog: MatDialogRef<CreateTerrariaInstanceDialogComponent>,
        private readonly restApi: RestApiService
    ) {}

    async ngOnInit(): Promise<void> {
        this.form.markAllAsTouched();
        this.tModLoaderVersions = await this.restApi.getTModLoaderVersions();
        this.tModLoaderVersionFormControl.enable();
        this.tModLoaderVersionFormControl.setValue(this.tModLoaderVersions[0].releaseId);
    }

    onCreateClicked(): void {
        this.creating = true;
        console.log('test');
        this.creating = false;
    }
}
