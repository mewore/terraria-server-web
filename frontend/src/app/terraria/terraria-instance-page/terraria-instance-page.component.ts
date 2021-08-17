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
} from 'src/generated/backend';
import { CreateWorldDialogService } from '../create-world-dialog/create-world-dialog.service';
import { RunServerDialogService } from '../run-server-dialog/run-server-dialog.service';
import { SetInstanceModsDialogService } from '../set-instance-mods-dialog/set-instance-mods-dialog.service';

interface LogPart {
    id: number;
    className: string;
    content: string;
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
            action: 'CREATE_WORLD',
            isDisplayed: () => this.instance?.state === 'WORLD_MENU',
            onClick: () => this.createWorld(),
        },
        {
            action: 'RUN_SERVER',
            isDisplayed: () => this.instance?.state === 'WORLD_MENU',
            onClick: () => this.runServer(),
        },
        {
            action: 'SHUT_DOWN',
            isDisplayed: () => !!this.instance && this.terrariaInstanceService.isActive(this.instance),
            onClick: () => this.shutDown(),
        },
        {
            action: 'TERMINATE',
            isDisplayed: () => !!this.instance && this.terrariaInstanceService.isActive(this.instance),
            onClick: () => this.terminate(),
            isWarn: true,
        },
        {
            action: 'RECREATE',
            isDisplayed: () => this.instance?.state === 'BROKEN',
            onClick: () => this.recreateInstance(),
        },
        {
            action: 'DELETE',
            isDisplayed: () => this.terrariaInstanceService.canDelete(this.instance),
            onClick: () => this.deleteInstance(),
            isWarn: true,
        },
    ];

    loading = true;

    deleted = false;

    host?: HostEntity;

    instance?: TerrariaInstanceEntity;

    logParts: LogPart[] = [];

    instanceId = 0;

    private instanceMessageSubscription?: Subscription;
    private instanceDeletionSubscription?: Subscription;
    private instanceEventMessageSubscription?: Subscription;

    constructor(
        private readonly restApi: RestApiService,
        private readonly activatedRoute: ActivatedRoute,
        private readonly errorService: ErrorService,
        private readonly authService: AuthenticationService,
        private readonly translateService: TranslateService,
        private readonly setInstanceModsDialogService: SetInstanceModsDialogService,
        private readonly createWorldDialogService: CreateWorldDialogService,
        private readonly runServerDialogService: RunServerDialogService,
        private readonly simpleDialogService: SimpleDialogService,
        private readonly terrariaInstanceService: TerrariaInstanceService,
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
                this.instanceDeletionSubscription = this.messageService.watchInstanceDeletion(instance).subscribe({
                    next: () => ((this.deleted = true), (this.instance = undefined)),
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
        this.instanceDeletionSubscription?.unsubscribe();
        this.instanceEventMessageSubscription?.unsubscribe();
    }

    get statusLabel(): string {
        return this.terrariaInstanceService.getStatusLabel(this.instance, this.deleted);
    }

    get badState(): boolean {
        return this.terrariaInstanceService.isStateBad(this.instance, this.deleted);
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

    createWorld(): void {
        const [instance, host] = [this.instance, this.host];
        if (!instance) {
            return this.errorService.showError('The instance is not defined!');
        }
        if (!host) {
            return this.errorService.showError('The host is not defined!');
        }
        this.doWhileLoading(() => this.createWorldDialogService.openDialog({ instance, hostId: host.id }));
    }

    runServer(): void {
        const [instance, host] = [this.instance, this.host];
        if (!instance) {
            return this.errorService.showError('The instance is not defined!');
        }
        if (!host) {
            return this.errorService.showError('The host is not defined!');
        }
        this.doWhileLoading(() => this.runServerDialogService.openDialog({ instance, hostId: host.id }));
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

    recreateInstance(): void {
        this.doWhileLoading(() => this.restApi.updateInstance(this.instanceId, { newAction: 'RECREATE' }));
    }

    deleteInstance(): void {
        this.doWhileLoading(() => this.terrariaInstanceService.delete(this.instance));
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
        const logPart = this.eventToLogPart({
            timestamp: '',
            ...eventMessage,
        });
        if (logPart) {
            this.logParts.push(logPart);
            // Normally, event messages come in the correct order, but that's not always the case
            let index = this.logParts.length - 1;
            while (index > 0 && this.logParts[index - 1].id > logPart.id) {
                this.logParts[index] = this.logParts[index - 1];
                index--;
            }
            this.logParts[index] = logPart;
            this.scrollToBottom();
        }
    }

    private async fetchData(hostId: number, instanceId: number): Promise<TerrariaInstanceEntity> {
        this.instanceId = instanceId;
        const [host, instanceDetails] = await Promise.all([
            this.restApi.getHost(hostId),
            this.restApi.getInstanceDetails(instanceId),
        ]);
        this.host = host;
        this.instance = instanceDetails.instance;
        this.logParts = instanceDetails.events
            .map((event) => this.eventToLogPart(event))
            .filter((part): part is LogPart => !!part);
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
                    id: event.id,
                    className: 'application-start green',
                    content: this.translateService.instant('terraria.instance.events.' + event.type),
                };
            }
            case 'APPLICATION_END': {
                return {
                    id: event.id,
                    className: 'application-end yellow',
                    content: this.translateService.instant('terraria.instance.events.' + event.type),
                };
            }
            case 'OUTPUT': {
                return {
                    id: event.id,
                    className: 'preformatted',
                    content: event.content,
                };
            }
            case 'IMPORTANT_OUTPUT': {
                return {
                    id: event.id,
                    className: 'important preformatted',
                    content: event.content,
                };
            }
            case 'ERROR':
            case 'TSW_INTERRUPTED':
            case 'INVALID_INSTANCE': {
                return {
                    id: event.id,
                    className: 'error orange',
                    content: this.translateService.instant('terraria.instance.events.' + event.type, {
                        error: event.content,
                    }),
                };
            }
            case 'INPUT': {
                return {
                    id: event.id,
                    className: 'input preformatted cyan',
                    content: event.content,
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
