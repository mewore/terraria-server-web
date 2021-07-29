import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import {
    HostEntity,
    TerrariaInstanceAction,
    TerrariaInstanceEntity,
    TerrariaInstanceEventEntity,
    TerrariaInstanceState,
} from 'src/generated/backend';
import { RunServerDialogService } from '../run-server-dialog/run-server-dialog.service';
import { SetInstanceModsDialogService } from '../set-instance-mods-dialog/set-instance-mods-dialog.service';

interface LogPart {
    className: string;
    text: string;
}

interface ButtonDefinition {
    action: TerrariaInstanceAction;
    isDisplayed: () => boolean;
    onClick: () => void;
    isWarn?: boolean;
}

@Component({
    selector: 'tsw-terraria-instance-page',
    templateUrl: './terraria-instance-page.component.html',
    styleUrls: ['./terraria-instance-page.component.sass'],
})
export class TerrariaInstancePageComponent implements AfterViewInit {
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

    readonly BUTTONS: ButtonDefinition[] = [
        {
            action: 'BOOT_UP',
            isDisplayed: () => this.instance?.state === 'IDLE',
            onClick: () => this.bootUp(),
        },
        {
            action: 'GO_TO_MOD_MENU',
            isDisplayed: () => this.instance?.state === 'WORLD_MENU',
            onClick: () => this.goToModMenu(),
        },
        {
            action: 'SET_LOADED_MODS',
            isDisplayed: () => this.instance?.state === 'MOD_MENU',
            onClick: () => this.setLoadedMods(),
        },
        {
            action: 'RUN_SERVER',
            isDisplayed: () => this.instance?.state === 'WORLD_MENU',
            onClick: () => this.runServer(),
        },
        {
            action: 'SHUT_DOWN',
            isDisplayed: () => !!this.instance && this.RUNNING_STATES.has(this.instance.state),
            onClick: () => this.shutDown(),
        },
        {
            action: 'TERMINATE',
            isDisplayed: () => !!this.instance && this.RUNNING_STATES.has(this.instance.state),
            onClick: () => this.terminate(),
            isWarn: true,
        },
        {
            action: 'DELETE',
            isDisplayed: () => !!this.instance && !this.RUNNING_STATES.has(this.instance.state),
            onClick: () => this.deleteInstance(),
            isWarn: true,
        },
    ];

    loading = true;

    host?: HostEntity;

    otherInstances?: TerrariaInstanceEntity[];

    instance?: TerrariaInstanceEntity;

    instanceEvents?: TerrariaInstanceEventEntity[];

    logParts: (LogPart | undefined)[] = [];

    instanceId = 0;

    constructor(
        private readonly restApi: RestApiService,
        private readonly activatedRoute: ActivatedRoute,
        private readonly authService: AuthenticationService,
        private readonly translateService: TranslateService,
        private readonly setInstanceModsDialogService: SetInstanceModsDialogService,
        private readonly runServerDialogService: RunServerDialogService,
        private readonly simpleDialogService: SimpleDialogService
    ) {}

    ngAfterViewInit(): void {
        this.activatedRoute.paramMap.subscribe(async (paramMap) => {
            try {
                const hostIdParam = paramMap.get('hostId');
                if (!hostIdParam) {
                    throw new Error('The [hostId] parameter is not set!');
                }
                const instanceIdParam = paramMap.get('instanceId');
                if (!instanceIdParam) {
                    throw new Error('The [instanceId] parameter is not set!');
                }
                const hostId = parseInt(hostIdParam, 10);
                const instanceId = parseInt(instanceIdParam, 10);
                this.fetchData(hostId, instanceId);
            } finally {
                this.loading = false;
            }
        });
    }

    private async fetchData(hostId: number, instanceId: number): Promise<void> {
        this.instanceId = instanceId;
        const [host, instances, instanceDetails] = await Promise.all([
            this.restApi.getHost(hostId),
            this.restApi.getHostInstances(hostId),
            this.restApi.getInstanceDetails(instanceId),
        ]);
        this.host = host;
        this.otherInstances = instances.filter((instance) => instance.id !== instanceId);
        this.instance = instances.find((instance) => instance.id === instanceId) || instanceDetails.instance;
        this.instanceEvents = instanceDetails.events;
        this.logParts = instanceDetails.events
            .map((event): LogPart | undefined => {
                switch (event.type) {
                    case 'APPLICATION_START': {
                        return {
                            className: 'application-start green',
                            text: this.translateService.instant('terraria.instance.events.' + event.type),
                        };
                    }
                    case 'APPLICATION_END': {
                        return {
                            className: 'application-end yellow',
                            text: this.translateService.instant('terraria.instance.events.' + event.type),
                        };
                    }
                    case 'OUTPUT': {
                        return {
                            className: 'preformatted',
                            text: event.text,
                        };
                    }
                    case 'IMPORTANT_OUTPUT': {
                        return {
                            className: 'important preformatted',
                            text: event.text,
                        };
                    }
                    case 'ERROR': {
                        return {
                            className: 'error orange',
                            text: this.translateService.instant('terraria.instance.events.' + event.type, {
                                error: event.text,
                            }),
                        };
                    }
                    case 'INVALID_INSTANCE': {
                        return {
                            className: 'error orange',
                            text: this.translateService.instant('terraria.instance.events.' + event.type, {
                                error: event.text,
                            }),
                        };
                    }
                    case 'INPUT': {
                        return {
                            className: 'input preformatted cyan',
                            text: event.text,
                        };
                    }
                    default: {
                        return undefined;
                    }
                }
            })
            .filter((part) => !!part);
        const panel = document.getElementById('instance-log-panel') as HTMLDivElement;
        setTimeout(() => panel.scrollTo({ top: panel.scrollHeight }), 100);
    }

    get loaded(): boolean {
        return !this.loading;
    }

    get hasAction(): boolean {
        return !!this.instance?.pendingAction || !!this.instance?.currentAction;
    }

    get canManageTerraria(): boolean {
        return this.authService.currentUser?.accountType?.ableToManageTerraria || false;
    }

    get canSetLoadedMods(): boolean {
        return !!this.instance && (this.instance.state === 'MOD_MENU' || this.instance.state === 'CHANGING_MOD_STATE');
    }

    get canRun(): boolean {
        return this.instance?.state === 'IDLE';
    }

    bootUp(): void {
        this.doWhileLoading(() => this.restApi.requestActionForInstance(this.instanceId, { action: 'BOOT_UP' }));
    }

    goToModMenu(): void {
        this.doWhileLoading(() => this.restApi.requestActionForInstance(this.instanceId, { action: 'GO_TO_MOD_MENU' }));
    }

    setLoadedMods(): void {
        const instance = this.instance;
        if (!instance) {
            throw new Error('The instance is not defined!');
        }
        this.doWhileLoading(() => this.setInstanceModsDialogService.openDialog(instance));
    }

    runServer(): void {
        const [instance, host] = [this.instance, this.host];
        if (!instance) {
            throw new Error('The instance is not defined!');
        }
        if (!host) {
            throw new Error('The host is not defined!');
        }
        this.doWhileLoading(() => this.runServerDialogService.openDialog({
            instance,
            hostId: host.id,
        }));
    }

    shutDown(): void {
        const instance = this.instance;
        if (!instance) {
            throw new Error('The instance is not defined!');
        }
        if (instance.state !== 'RUNNING') {
            this.doWhileLoading(() => this.restApi.requestActionForInstance(this.instanceId, { action: 'SHUT_DOWN' }));
            return;
        }
        this.doWhileLoading(() => {
            return this.simpleDialogService.openDialog<TerrariaInstanceEntity>({
                titleKey: 'terraria.instance.dialog.shut-down.title',
                descriptionKey: 'terraria.instance.dialog.shut-down.description', 
                primaryButton: {
                    labelKey: 'terraria.instance.dialog.shut-down.confirm',
                    onClicked: () => this.restApi.requestActionForInstance(this.instanceId, { action: 'SHUT_DOWN' }),
                },
                extraButtons: [{
                    labelKey: 'terraria.instance.dialog.shut-down.confirm-no-save',
                    onClicked: () => this.restApi.requestActionForInstance(this.instanceId, { action: 'SHUT_DOWN_NO_SAVE' }),
                }],
            });
        });
    }

    terminate(): void {
        this.doWhileLoading(() => {
            return this.simpleDialogService.openDialog<TerrariaInstanceEntity>({
                titleKey: 'terraria.instance.dialog.terminate.title',
                descriptionKey: 'terraria.instance.dialog.terminate.description', 
                primaryButton: {
                    labelKey: 'terraria.instance.dialog.terminate.confirm',
                    onClicked: () => this.restApi.requestActionForInstance(this.instanceId, { action: 'TERMINATE' }),
                },
                warn: true,
            });
        });
    }

    deleteInstance(): void {
        this.doWhileLoading(() => {
            return this.simpleDialogService.openDialog<TerrariaInstanceEntity>({
                titleKey: 'terraria.instance.dialog.delete.title',
                descriptionKey: 'terraria.instance.dialog.delete.description', 
                primaryButton: {
                    labelKey: 'terraria.instance.dialog.delete.confirm',
                    onClicked: () => this.restApi.requestActionForInstance(this.instanceId, { action: 'DELETE' }),
                },
                warn: true,
            });
        });
    }

    async doWhileLoading(action: () => undefined | Promise<TerrariaInstanceEntity | undefined>): Promise<void> {
        if (this.loading) {
            throw new Error('Already loading!');
        }
        this.loading = true;
        try {
            const result = action();
            if (result) {
                this.instance = (await result) ?? this.instance;
            }
        } finally {
            this.loading = false;
        }
    }
}
