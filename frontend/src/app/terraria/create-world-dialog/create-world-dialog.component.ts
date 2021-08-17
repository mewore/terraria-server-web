import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, Validators, FormGroup } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TswValidators } from 'src/app/core/tsw-validators';
import {
    TerrariaInstanceEntity,
    WorldSizeOption,
    WorldDifficultyOption,
    TerrariaWorldEntity,
} from 'src/generated/backend';

export interface CreateWorldDialogInput {
    instance: TerrariaInstanceEntity;
    hostId: number;
}

export type CreateWorldDialogOutput = TerrariaInstanceEntity;

@Component({
    selector: 'tsw-create-world-dialog',
    templateUrl: './create-world-dialog.component.html',
    styleUrls: ['./create-world-dialog.component.sass'],
})
export class CreateWorldDialogComponent implements OnInit {
    readonly MAX_NAME_LENGTH = 255;

    loading = false;

    readonly nameInput = new FormControl('', [
        TswValidators.notBlank,
        Validators.maxLength(this.MAX_NAME_LENGTH),
        () => (this.worldFileNames && this.worldFileNames.has(this.fileName) ? { duplicate: true } : null),
    ]);

    readonly sizeOptions: ReadonlyArray<WorldSizeOption> = ['SMALL', 'MEDIUM', 'LARGE'];
    readonly sizeInput = new FormControl(this.sizeOptions[0], [Validators.required]);

    readonly difficultyOptions: ReadonlyArray<WorldDifficultyOption> = ['NORMAL', 'EXPERT'];
    readonly difficultyInput = new FormControl(this.difficultyOptions[0], [Validators.required]);

    readonly form = new FormGroup({
        name: this.nameInput,
        size: this.sizeInput,
        difficulty: this.difficultyInput,
    } as { [key: string]: FormControl });

    private readonly instanceId: number;

    private worldFileNames = new Set<string>();

    constructor(
        private readonly dialog: MatDialogRef<CreateWorldDialogComponent, CreateWorldDialogOutput>,
        private readonly restApi: RestApiService,
        @Inject(MAT_DIALOG_DATA) private readonly data: CreateWorldDialogInput
    ) {
        this.instanceId = data.instance.id;
    }

    get fileName(): string {
        return this.displayNameToFileName(this.displayName).trim();
    }

    get displayName(): string {
        return ('' + this.nameInput.value).trim();
    }

    async ngOnInit(): Promise<void> {
        this.form.markAllAsTouched();
        try {
            this.loading = true;
            const worlds = await this.restApi.getHostWorlds(this.data.hostId);
            this.worldFileNames = new Set<string>(worlds.map((world) => this.displayNameToFileName(world.displayName)));
        } finally {
            this.loading = false;
        }
        this.sizeInput.enable();
    }

    async onCreateClicked(): Promise<void> {
        if (this.form.invalid) {
            this.form.markAsTouched();
            return;
        }

        this.loading = true;
        try {
            const newInstance = await this.restApi.updateInstance(this.instanceId, {
                worldCreationConfiguration: {
                    worldSize: this.sizeInput.value,
                    worldDifficulty: this.difficultyInput.value,
                    worldDisplayName: this.displayName,
                },
            });
            this.dialog.close(newInstance);
        } finally {
            this.loading = false;
        }
    }

    private displayNameToFileName(displayName: string): string {
        return displayName.replace(/ /g, '_');
    }
}
