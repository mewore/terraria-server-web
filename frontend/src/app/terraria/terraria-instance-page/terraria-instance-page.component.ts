import { AfterViewInit, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErrorService } from 'src/app/core/services/error.service';
import { MessageService } from 'src/app/core/services/message.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { TerrariaInstanceService } from 'src/app/terraria-core/services/terraria-instance.service';
import {
    HostEntity,
    TerrariaInstanceAction,
    TerrariaInstanceEntity,
    TerrariaInstanceEventEntity,
    TerrariaInstanceEventMessage,
    TerrariaInstanceMessage,
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
export class TerrariaInstancePageComponent implements AfterViewInit, OnDestroy {
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
            onClick: () => this.setEnabledMods(),
        },
        {
            action: 'RUN_SERVER',
            isDisplayed: () => this.instance?.state === 'WORLD_MENU',
            onClick: () => this.runServer(),
        },
        {
            action: 'SHUT_DOWN',
            isDisplayed: () => !!this.instance && this.terarriaInstanceService.isRunning(this.instance),
            onClick: () => this.shutDown(),
        },
        {
            action: 'TERMINATE',
            isDisplayed: () => !!this.instance && this.terarriaInstanceService.isRunning(this.instance),
            onClick: () => this.terminate(),
            isWarn: true,
        },
        {
            action: 'DELETE',
            isDisplayed: () => this.terarriaInstanceService.canDelete(this.instance),
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

    private instanceMessageSubscription?: Subscription;

    private instanceEventMessageSubscription?: Subscription;

    constructor(
        private readonly restApi: RestApiService,
        private readonly activatedRoute: ActivatedRoute,
        private readonly errorService: ErrorService,
        private readonly authService: AuthenticationService,
        private readonly translateService: TranslateService,
        private readonly runServerDialogService: RunServerDialogService,
        private readonly setInstanceModsDialogService: SetInstanceModsDialogService,
        private readonly simpleDialogService: SimpleDialogService,
        private readonly terarriaInstanceService: TerrariaInstanceService,
        private readonly messageService: MessageService
    ) {}

    ngAfterViewInit(): void {
        this.activatedRoute.paramMap.subscribe(async (paramMap) => {
            try {
                const hostIdParam = paramMap.get('hostId');
                if (!hostIdParam) {
                    this.errorService.showError('The [hostId] parameter is not set!');
                    return;
                }
                const instanceIdParam = paramMap.get('instanceId');
                if (!instanceIdParam) {
                    this.errorService.showError('The [instanceId] parameter is not set!');
                    return;
                }
                const hostId = parseInt(hostIdParam, 10);
                const instanceId = parseInt(instanceIdParam, 10);
                const instance = await this.fetchData(hostId, instanceId);
                this.instanceMessageSubscription = this.messageService.watchInstanceChanges(instance).subscribe({
                    next: (instanceMessage) => this.updateInstance(instanceMessage),
                });
                this.instanceEventMessageSubscription = this.messageService.watchInstanceEvents(instance).subscribe({
                    next: (eventMessage) => this.addInstanceEvent(eventMessage),
                });
            } finally {
                this.loading = false;
            }
        });
    }

    ngOnDestroy(): void {
        this.instanceMessageSubscription?.unsubscribe();
        this.instanceEventMessageSubscription?.unsubscribe();
    }

    get hasAction(): boolean {
        return !!this.instance?.pendingAction || !!this.instance?.currentAction;
    }

    get canManageTerraria(): boolean {
        return this.authService.currentUser?.accountType?.ableToManageTerraria || false;
    }

    bootUp(): void {
        this.doWhileLoading(() => this.restApi.updateInstance(this.instanceId, { newAction: 'BOOT_UP' }));
    }

    goToModMenu(): void {
        this.doWhileLoading(() => this.restApi.updateInstance(this.instanceId, { newAction: 'GO_TO_MOD_MENU' }));
    }

    setEnabledMods(): void {
        const instance = this.instance;
        if (!instance) {
            return this.errorService.showError('The instance is not defined!');
        }
        this.doWhileLoading(() => this.setInstanceModsDialogService.openDialog(instance));
    }

    runServer(): void {
        const [instance, host] = [this.instance, this.host];
        if (!instance) {
            return this.errorService.showError('The instance is not defined!');
        }
        if (!host) {
            return this.errorService.showError('The host is not defined!');
        }
        this.doWhileLoading(() =>
            this.runServerDialogService.openDialog({
                instance,
                hostId: host.id,
            })
        );
    }

    shutDown(): void {
        const instance = this.instance;
        if (!instance) {
            return this.errorService.showError('The instance is not defined!');
        }
        if (instance.state !== 'RUNNING') {
            this.doWhileLoading(() => this.restApi.updateInstance(this.instanceId, { newAction: 'SHUT_DOWN' }));
            return;
        }
        this.doWhileLoading(() =>
            this.simpleDialogService.openDialog<TerrariaInstanceEntity>({
                titleKey: 'terraria.instance.dialog.shut-down.title',
                descriptionKey: 'terraria.instance.dialog.shut-down.description',
                primaryButton: {
                    labelKey: 'terraria.instance.dialog.shut-down.confirm',
                    onClicked: () => this.restApi.updateInstance(this.instanceId, { newAction: 'SHUT_DOWN' }),
                },
                extraButtons: [
                    {
                        labelKey: 'terraria.instance.dialog.shut-down.confirm-no-save',
                        onClicked: () =>
                            this.restApi.updateInstance(this.instanceId, { newAction: 'SHUT_DOWN_NO_SAVE' }),
                    },
                ],
            })
        );
    }

    terminate(): void {
        this.doWhileLoading(() =>
            this.simpleDialogService.openDialog<TerrariaInstanceEntity>({
                titleKey: 'terraria.instance.dialog.terminate.title',
                descriptionKey: 'terraria.instance.dialog.terminate.description',
                primaryButton: {
                    labelKey: 'terraria.instance.dialog.terminate.confirm',
                    onClicked: () => this.restApi.updateInstance(this.instanceId, { newAction: 'TERMINATE' }),
                },
                warn: true,
            })
        );
    }

    deleteInstance(): void {
        this.doWhileLoading(() => this.terarriaInstanceService.delete(this.instance));
    }

    private updateInstance(instanceMessage: TerrariaInstanceMessage): void {
        const instance: TerrariaInstanceEntity | undefined = this.instance;
        if (instance) {
            this.instance = {
                ...instance,
                ...instanceMessage,
            };
        }
    }

    private addInstanceEvent(eventMessage: TerrariaInstanceEventMessage): void {
        const event: TerrariaInstanceEventEntity = {
            timestamp: '',
            ...eventMessage,
        };
        this.instanceEvents?.push(event);
        const logPart = this.eventToLogPart(event);
        if (logPart) {
            this.logParts.push(logPart);
            this.scrollToBottom();
        }
    }

    private async fetchData(hostId: number, instanceId: number): Promise<TerrariaInstanceEntity> {
        this.instanceId = instanceId;
        const [host, instances, instanceDetails] = await Promise.all([
            this.restApi.getHost(hostId),
            this.restApi.getHostInstances(hostId),
            this.restApi.getInstanceDetails(instanceId),
        ]);
        this.host = host;
        this.otherInstances = instances.filter((instance) => instance.id !== instanceId);
        this.instance = instanceDetails.instance;
        this.instanceEvents = instanceDetails.events;
        this.logParts = instanceDetails.events.map((event) => this.eventToLogPart(event)).filter((part) => !!part);
        this.scrollToBottom();
        return instanceDetails.instance;
    }

    private scrollToBottom(): void {
        const panel = document.getElementById('instance-log-panel') as HTMLDivElement;
        setTimeout(() => panel.scrollTo({ top: panel.scrollHeight }), 100);
    }

    private eventToLogPart(event: TerrariaInstanceEventEntity): LogPart | undefined {
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
            case 'ERROR':
            case 'TSW_INTERRUPTED':
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
    }

    private async doWhileLoading(action: () => Promise<TerrariaInstanceEntity | undefined>): Promise<void> {
        if (this.loading) {
            return this.errorService.showError('Already loading!');
        }
        this.loading = true;
        try {
            this.instance = (await action()) ?? this.instance;
        } finally {
            this.loading = false;
        }
    }
}
