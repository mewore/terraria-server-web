import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { TswValidators } from 'src/app/core/tsw-validators';
import { TerrariaInstanceEntity, TerrariaWorldEntity } from 'src/generated/backend';

export interface RunServerDialogInput {
    instance: TerrariaInstanceEntity;
    hostId: number;
}

export type RunServerDialogOutput = TerrariaInstanceEntity;

@Component({
    selector: 'tsw-run-server-dialog',
    templateUrl: './run-server-dialog.component.html',
    styleUrls: ['./run-server-dialog.component.sass'],
})
export class RunServerDialogComponent implements OnInit {
    readonly MIN_PLAYER_LIMIT = 0;
    readonly MAX_PLAYER_LIMIT = 1000000000;

    readonly MIN_PORT = 1024;
    readonly MAX_PORT = 49151;

    readonly MAX_PASSWORD_LENGTH = 255;

    private readonly instanceId: number;

    loading = false;

    readonly maxPlayersInput = new FormControl(8, [
        Validators.required,
        Validators.min(this.MIN_PLAYER_LIMIT),
        Validators.max(this.MAX_PLAYER_LIMIT),
    ]);

    readonly takenPorts: Set<number> = new Set<number>();
    readonly portInput = new FormControl(7777, [
        Validators.required,
        Validators.min(this.MIN_PORT),
        Validators.max(this.MAX_PORT),
        TswValidators.noDuplicates<number>(this.takenPorts),
    ]);

    readonly automaticallyForwardPortInput = new FormControl(true);

    readonly passwordInput = new FormControl('', [Validators.maxLength(this.MAX_PASSWORD_LENGTH)]);

    worlds: TerrariaWorldEntity[] = [];
    worldById: { [id: number]: TerrariaWorldEntity } = {};
    private worldModStringById: { [id: number]: string } = {};
    private readonly instancesByWorldId = new Map<number, TerrariaInstanceEntity[]>();
    readonly instanceModString: string;

    readonly worldInput = new FormControl({ value: undefined, disabled: true }, [Validators.required]);

    readonly form = new FormGroup({
        maxPlayers: this.maxPlayersInput,
        port: this.portInput,
        automaticallyForwardPort: this.automaticallyForwardPortInput,
        password: this.passwordInput,
        world: this.worldInput,
    } as { [key: string]: FormControl });

    constructor(
        private readonly dialog: MatDialogRef<RunServerDialogComponent, RunServerDialogOutput>,
        private readonly restApi: RestApiService,
        @Inject(MAT_DIALOG_DATA) private readonly data: RunServerDialogInput
    ) {
        this.instanceId = data.instance.id;
        this.instanceModString = data.instance.loadedMods.sort().join(', ');
    }

    get selectedWorldModString(): string | undefined {
        return this.worldModStringById[this.worldInput.value] ?? undefined;
    }

    get worldHasMismatchingMods(): boolean {
        const worldModString = this.selectedWorldModString;
        return worldModString != null && this.instanceModString !== worldModString;
    }

    get worldHasUnknownMods(): boolean {
        return this.worldInput.value != null && this.selectedWorldModString == null;
    }

    worldIsUsedByServer(world: TerrariaWorldEntity): boolean {
        return this.instancesByWorldId.has(world.id);
    }

    get selectedWorldIsUsedByServer(): boolean {
        return this.instancesByWorldId.has(this.worldInput.value);
    }

    async ngOnInit(): Promise<void> {
        this.form.markAllAsTouched();
        try {
            this.loading = true;
            this.worldModStringById = {};
            this.worldById = {};
            const [instances, worlds] = await Promise.all([
                this.restApi.getHostInstances(this.data.hostId),
                this.restApi.getHostWorlds(this.data.hostId),
            ]);
            this.takenPorts.clear();
            for (const instance of instances) {
                if (instance.state === 'RUNNING') {
                    this.takenPorts.add(instance.port);
                    if (instance.worldId) {
                        const instanceArray = this.instancesByWorldId.get(instance.worldId);
                        if (instanceArray) {
                            instanceArray.push(instance);
                        } else {
                            this.instancesByWorldId.set(instance.worldId, [instance]);
                        }
                    }
                }
            }
            this.portInput.setValue(this.portInput.value);
            this.worlds = worlds;
            for (const world of this.worlds) {
                this.worldById[world.id] = world;
                const mods = world.mods;
                if (mods) {
                    this.worldModStringById[world.id] = mods.sort().join(', ');
                }
            }
        } finally {
            this.loading = false;
        }
        if (this.worlds.length) {
            this.worldInput.setValue(this.worlds[0].id);
        }
        this.worldInput.enable();
    }

    async onRunClicked(): Promise<void> {
        if (this.form.invalid) {
            this.form.markAsTouched();
            return;
        }

        this.loading = true;
        try {
            const newInstance = await this.restApi.runInstance(this.instanceId, {
                maxPlayers: this.maxPlayersInput.value,
                port: this.portInput.value,
                automaticallyForwardPort: this.automaticallyForwardPortInput.value,
                password: this.passwordInput.value,
                worldId: this.worldInput.value,
            });
            this.dialog.close(newInstance);
        } finally {
            this.loading = false;
        }
    }
}
