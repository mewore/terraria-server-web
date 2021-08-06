import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { EMPTY, Observable, Subject } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { MessageService } from 'src/app/core/services/message.service';
import { MessageServiceStub } from 'src/app/core/services/message.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { SimpleDialogInput } from 'src/app/core/simple-dialog/simple-dialog.component';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { SimpleDialogServiceStub } from 'src/app/core/simple-dialog/simple-dialog.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import {
    HostEntity,
    TerrariaInstanceAction,
    TerrariaInstanceDetailsViewModel,
    TerrariaInstanceEntity,
    TerrariaInstanceEventEntity,
    TerrariaInstanceEventMessage,
    TerrariaInstanceEventType,
    TerrariaInstanceMessage,
} from 'src/generated/backend';
import { FakeParamMap } from 'src/stubs/fake-param-map';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { TranslateServiceStub } from 'src/stubs/translate.service.stub';
import { RunServerDialogInput } from '../run-server-dialog/run-server-dialog.component';
import { RunServerDialogService } from '../run-server-dialog/run-server-dialog.service';
import { RunServerDialogServiceStub } from '../run-server-dialog/run-server-dialog.service.stub';
import { SetInstanceModsDialogInput } from '../set-instance-mods-dialog/set-instance-mods-dialog.component';
import { SetInstanceModsDialogService } from '../set-instance-mods-dialog/set-instance-mods-dialog.service';
import { SetInstanceModsDialogServiceStub } from '../set-instance-mods-dialog/set-instance-mods-dialog.service.stub';

import { TerrariaInstancePageComponent } from './terraria-instance-page.component';

describe('TerrariaInstancePageComponent', () => {
    let component: TerrariaInstancePageComponent;
    let fixture: ComponentFixture<TerrariaInstancePageComponent>;

    let restApiService: RestApiService;
    let routeSubject: Subject<ParamMap>;
    let errorService: ErrorService;
    let authenticationService: AuthenticationService;
    let translateService: TranslateService;
    let runServerDialogService: RunServerDialogService;
    let setInstanceModsDialogService: SetInstanceModsDialogService;
    let simpleDialogService: SimpleDialogService;
    let messageService: MessageService;

    let authenticatedUser: AuthenticatedUser | undefined;

    async function instantiate(): Promise<void> {
        fixture = TestBed.createComponent(TerrariaInstancePageComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        component = fixture.componentInstance;
    }

    let instanceEvents: TerrariaInstanceEventEntity[];
    let instance: TerrariaInstanceEntity;
    let hostInstances: TerrariaInstanceEntity[];

    let getHostSpy: jasmine.Spy<(hostId: number) => Promise<HostEntity>>;
    let getInstancesSpy: jasmine.Spy<(hostId: number) => Promise<TerrariaInstanceEntity[]>>;
    let getInstanceDetailsSpy: jasmine.Spy<(instanceId: number) => Promise<TerrariaInstanceDetailsViewModel>>;

    let watchInstanceChangesSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => Observable<TerrariaInstanceMessage>>;
    let instanceMessageSubject: Subject<TerrariaInstanceMessage>;
    let watchInstanceEventsSpy: jasmine.Spy<
        (instance: TerrariaInstanceEntity) => Observable<TerrariaInstanceEventMessage>
    >;
    let eventMessageSubject: Subject<TerrariaInstanceEventMessage>;

    function inputEvent(text: string): TerrariaInstanceEventEntity {
        return { text, type: 'INPUT' } as TerrariaInstanceEventEntity;
    }

    function outputEvent(text: string): TerrariaInstanceEventEntity {
        return { text, type: 'OUTPUT' } as TerrariaInstanceEventEntity;
    }

    beforeEach(async () => {
        routeSubject = new Subject();

        await TestBed.configureTestingModule({
            imports: [MatTooltipModule, MatButtonModule, MatProgressBarModule],
            declarations: [TerrariaInstancePageComponent, TranslatePipeStub],
            providers: [
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: ActivatedRoute, useValue: { paramMap: routeSubject.asObservable() } },
                { provide: ErrorService, useClass: ErrorServiceStub },
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: TranslateService, useClass: TranslateServiceStub },
                { provide: RunServerDialogService, useClass: RunServerDialogServiceStub },
                { provide: SetInstanceModsDialogService, useClass: SetInstanceModsDialogServiceStub },
                { provide: SimpleDialogService, useClass: SimpleDialogServiceStub },
                { provide: MessageService, useClass: MessageServiceStub },
            ],
        }).compileComponents();

        restApiService = TestBed.inject(RestApiService);
        errorService = TestBed.inject(ErrorService);
        authenticationService = TestBed.inject(AuthenticationService);
        translateService = TestBed.inject(TranslateService);
        runServerDialogService = TestBed.inject(RunServerDialogService);
        setInstanceModsDialogService = TestBed.inject(SetInstanceModsDialogService);
        simpleDialogService = TestBed.inject(SimpleDialogService);
        messageService = TestBed.inject(MessageService);

        instanceEvents = [];
        instance = { id: 20, state: 'DEFINED' } as TerrariaInstanceEntity;
        hostInstances = [
            { id: instance.id, state: 'IDLE' } as TerrariaInstanceEntity,
            { id: 2 } as TerrariaInstanceEntity,
        ];

        getHostSpy = spyOn(restApiService, 'getHost').and.resolveTo({ id: 10 } as HostEntity);
        getInstancesSpy = spyOn(restApiService, 'getHostInstances').and.callFake(async () => hostInstances);
        getInstanceDetailsSpy = spyOn(restApiService, 'getInstanceDetails').and.callFake(() =>
            Promise.resolve({ events: instanceEvents, instance })
        );
        authenticatedUser = {
            accountType: {
                ableToManageAccounts: false,
                ableToManageHosts: false,
                ableToManageTerraria: true,
            },
        } as AuthenticatedUser;
        spyOnProperty(authenticationService, 'currentUser', 'get').and.callFake(() => authenticatedUser);

        instanceMessageSubject = new Subject();
        watchInstanceChangesSpy = spyOn(messageService, 'watchInstanceChanges').and.returnValue(
            instanceMessageSubject.asObservable()
        );
        eventMessageSubject = new Subject();
        watchInstanceEventsSpy = spyOn(messageService, 'watchInstanceEvents').and.returnValue(
            eventMessageSubject.asObservable()
        );

        await instantiate();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('when there is an invalid activated route', () => {
        describe('when the route is with an invalid host ID', () => {
            let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

            beforeEach(fakeAsync(() => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                routeSubject.next(new FakeParamMap({ instanceId: '20' }));
                flushMicrotasks();
            }));

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith('The [hostId] parameter is not set!');
            });

            it('should not fetch any data', () => {
                expect(getHostSpy).not.toHaveBeenCalled();
                expect(getInstancesSpy).not.toHaveBeenCalled();
                expect(getInstanceDetailsSpy).not.toHaveBeenCalled();
                expect(watchInstanceChangesSpy).not.toHaveBeenCalled();
                expect(watchInstanceEventsSpy).not.toHaveBeenCalled();
            });
        });

        describe('when the route is with an invalid instance ID', () => {
            let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

            beforeEach(fakeAsync(() => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                routeSubject.next(new FakeParamMap({ hostId: '10' }));
                flushMicrotasks();
            }));

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith('The [instanceId] parameter is not set!');
            });
        });
    });

    const initializeWithRoute = fakeAsync(async () => {
        routeSubject.next(new FakeParamMap({ hostId: '10', instanceId: '20' }));
        fixture.detectChanges();
        tick(1000);
        fixture.detectChanges();
        tick();
        await fixture.whenStable();
    });

    function getButton(label: string): HTMLButtonElement {
        const buttons = (fixture.nativeElement as Element).getElementsByTagName('button');
        for (let i = 0; i < buttons.length; i++) {
            const button = buttons.item(i);
            if (button && button.innerText.trim() === label) {
                return button;
            }
        }
        throw new Error(`There is no '${label}' button`);
    }

    function getButtonLabels(): string[] {
        const buttons = (fixture.nativeElement as Element).getElementsByTagName('button');
        const result: string[] = [];
        for (let i = 0; i < buttons.length; i++) {
            const button = buttons.item(i);
            if (button) {
                result.push(button.innerText);
            }
        }
        return result;
    }

    function getInstanceInfoText(): string {
        const infoBar = (fixture.nativeElement as Element).getElementsByClassName('instance-info-bar').item(0);
        if (!infoBar) {
            throw new Error('There is no instance-info-bar element');
        }
        return (infoBar as HTMLElement).innerText;
    }

    describe('when there is minimal data', () => {
        beforeEach(initializeWithRoute);

        describe('when there is an instance change message', () => {
            beforeEach(fakeAsync(() => {
                instanceMessageSubject.next({
                    state: 'BOOTING_UP',
                    currentAction: 'BOOT_UP',
                    pendingAction: 'RUN_SERVER',
                    options: { 1: 'option' },
                });
                tick(1000);
            }));

            it('should update the instance', () => {
                expect(component.instance?.state).toBe('BOOTING_UP');
                expect(component.instance?.currentAction).toBe('BOOT_UP');
                expect(component.instance?.pendingAction).toBe('RUN_SERVER');
                expect(component.instance?.options).toEqual({ 1: 'option' });
            });
        });

        describe('when the instance property is not defined', () => {
            beforeEach(() => {
                component.instance = undefined;
            });

            describe('when there is an instance change message', () => {
                beforeEach(fakeAsync(() => {
                    instanceMessageSubject.next({
                        state: 'BOOTING_UP',
                        currentAction: 'BOOT_UP',
                        pendingAction: 'RUN_SERVER',
                        options: { 1: 'option' },
                    });
                    tick(1000);
                }));

                it('should not update the instance', () => {
                    expect(component.instance).toBeUndefined();
                });
            });
        });

        describe('when there is an instance event message', () => {
            beforeEach(fakeAsync(() => {
                eventMessageSubject.next({
                    text: 'new output\n',
                    type: 'OUTPUT',
                });
                tick(1000);
            }));

            it('should update the event array', () => {
                expect(component.instanceEvents).toEqual([
                    {
                        text: 'new output\n',
                        type: 'OUTPUT',
                        timestamp: '',
                    },
                ]);
                expect(component.logParts).toEqual([
                    {
                        text: 'new output\n',
                        className: 'preformatted',
                    },
                ]);
            });
        });

        describe('when the instanceEvents property is not defined', () => {
            beforeEach(() => {
                component.instanceEvents = undefined;
            });

            describe('when there is an instance event message', () => {
                beforeEach(fakeAsync(() => {
                    eventMessageSubject.next({
                        text: 'new output\n',
                        type: 'OUTPUT',
                    });
                    tick(1000);
                }));

                it('should update only the log part array', () => {
                    expect(component.logParts).toEqual([
                        {
                            text: 'new output\n',
                            className: 'preformatted',
                        },
                    ]);
                });
            });
        });

        describe('when there is an instance event message with an unknown type', () => {
            beforeEach(fakeAsync(() => {
                eventMessageSubject.next({
                    text: 'new output\n',
                    type: 'INVALID_TYPE' as TerrariaInstanceEventType,
                });
                tick(1000);
            }));

            it('should not update only the event array', () => {
                expect(component.instanceEvents).toEqual([
                    {
                        text: 'new output\n',
                        type: 'INVALID_TYPE' as TerrariaInstanceEventType,
                        timestamp: '',
                    },
                ]);
                expect(component.logParts).toEqual([]);
            });
        });

        it('should fetch the data', () => {
            expect(getHostSpy).toHaveBeenCalledOnceWith(10);
            expect(getInstancesSpy).toHaveBeenCalledOnceWith(10);
            expect(getInstanceDetailsSpy).toHaveBeenCalledOnceWith(20);
        });

        it('should set the host', () => {
            expect(component.host?.id).toBe(10);
        });

        it('should set the other instances', () => {
            expect(component.otherInstances).toEqual([{ id: 2 } as TerrariaInstanceEntity]);
        });

        it('should set the instance from the host instances', () => {
            expect(component.instance).toBe(instance);
        });
    });

    describe('when there are all kinds of instance events', () => {
        function importantEvent(text: string): TerrariaInstanceEventEntity {
            return { text, type: 'IMPORTANT_OUTPUT' } as TerrariaInstanceEventEntity;
        }
        function detailEvent(text: string): TerrariaInstanceEventEntity {
            return { text, type: 'DETAILED_OUTPUT' } as TerrariaInstanceEventEntity;
        }

        function getLogLines(): string[] {
            const logPanel = (fixture.nativeElement as Element).getElementsByClassName('log-panel').item(0);
            if (!logPanel) {
                throw new Error('There is no log-panel element');
            }
            return (logPanel as HTMLElement).innerText.split('\n');
        }

        beforeEach(() => {
            instanceEvents = [
                {
                    text: 'The Terraria server URL is unreachable!',
                    type: 'INVALID_INSTANCE',
                } as TerrariaInstanceEventEntity,
                { text: 'Something Went Wrong (TM)', type: 'ERROR' } as TerrariaInstanceEventEntity,
                { text: 'Oof.', type: 'TSW_INTERRUPTED' } as TerrariaInstanceEventEntity,
                { type: 'APPLICATION_START' } as TerrariaInstanceEventEntity,
                outputEvent('Unloading mods...\n'),
                importantEvent('Finding Mods...'),
                outputEvent('\nSandboxing: FirstMod\nSandboxing: SecondMod\n'),
                detailEvent('Some detail\n'),
                detailEvent('Another detail\n'),
                outputEvent('Setting up...\n'),
                detailEvent('A third detail\n'),
                importantEvent('Choose World: '),
                inputEvent('m'),
                outputEvent('\n'),
                outputEvent('\n\n'),
                { type: 'APPLICATION_END' } as TerrariaInstanceEventEntity,
                { type: 'INVALID_EVENT_TYPE!!!' } as any,
            ];
            initializeWithRoute();
        });

        it('should have the correct log text', () => {
            expect(getLogLines()).toEqual([
                'The instance has been determined to be invalid: The Terraria server URL is unreachable!',
                'Error: Something Went Wrong (TM)',
                'The application was interrupted: Oof.',
                'Application started.',
                'Unloading mods...',
                'Finding Mods...',
                'Sandboxing: FirstMod',
                'Sandboxing: SecondMod',
                'Setting up...',
                'Choose World: m',
                '',
                '',
                '',
                'Application stopped.',
                'The instance has been defined.',
                'Delete',
            ]);
        });
    });

    type RequestActionFn = (
        instanceId: number,
        queryParams: { action: TerrariaInstanceAction }
    ) => Promise<TerrariaInstanceEntity>;

    describe('when there is no account', () => {
        beforeEach(() => {
            authenticatedUser = undefined;
            initializeWithRoute();
        });

        describe('the "Delete" button', () => {
            it('should be disabled', () => {
                expect(getButton('Delete').getAttribute('disabled')).toBe('true');
            });
        });
    });

    describe('when there is an account with no account type', () => {
        beforeEach(() => {
            authenticatedUser = {} as AuthenticatedUser;
            initializeWithRoute();
        });

        describe('the "Delete" button', () => {
            it('should be disabled', () => {
                expect(getButton('Delete').getAttribute('disabled')).toBe('true');
            });
        });
    });

    describe('when there is an account with an account type without the manage terraria permission', () => {
        beforeEach(() => {
            authenticatedUser = {
                accountType: {
                    ableToManageAccounts: true,
                    ableToManageHosts: true,
                    ableToManageTerraria: false,
                },
            } as AuthenticatedUser;
            initializeWithRoute();
        });

        describe('the "Delete" button', () => {
            it('should be disabled', () => {
                expect(getButton('Delete').getAttribute('disabled')).toBe('true');
            });
        });
    });

    describe('when the instance has the action of the instance changes', () => {
        beforeEach(() => {
            initializeWithRoute();
        });

        it('its text should change too', () => {
            const infoTextWithoutAction = 'The instance has been defined.';
            for (const action of [
                'DELETE',
                'GO_TO_MOD_MENU',
                'RUN_SERVER',
                'SET_LOADED_MODS',
                'SET_UP',
                'SHUT_DOWN',
                'SHUT_DOWN_NO_SAVE',
                'TERMINATE',
            ] as TerrariaInstanceAction[]) {
                instance.pendingAction = action;
                fixture.detectChanges();
                const infoTextWithPendingAction = getInstanceInfoText();
                expect(infoTextWithPendingAction).not.toBe(infoTextWithoutAction);

                instance.pendingAction = undefined;
                instance.currentAction = action;
                fixture.detectChanges();
                expect(getInstanceInfoText()).not.toBe(infoTextWithoutAction);
                expect(getInstanceInfoText()).not.toBe(infoTextWithPendingAction);

                instance.currentAction = undefined;
                fixture.detectChanges();
                expect(getInstanceInfoText()).toBe(infoTextWithoutAction);
            }
        });
    });

    describe('when the instance is in the DEFINED state', () => {
        beforeEach(() => {
            instance.state = 'DEFINED';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance has been defined.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Delete']);
        });

        describe('when the "Delete" button is clicked', () => {
            describe('when the confirmation dialog is confirmed', () => {
                let simpleDialogData: SimpleDialogInput<TerrariaInstanceEntity>;
                let requestActionSpy: jasmine.Spy<RequestActionFn>;
                const newInstance = {} as TerrariaInstanceEntity;

                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.callFake(async <T>(data: SimpleDialogInput<T>) => {
                        simpleDialogData = data as SimpleDialogInput<any>;
                        return await data.primaryButton.onClicked();
                    });
                    requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                    getButton('Delete').click();
                    tick();
                }));

                it('should open the confirmation dialog with the correct data', () => {
                    expect([
                        translateService.instant(simpleDialogData.titleKey),
                        translateService.instant(simpleDialogData.descriptionKey || 'NO_DESCRIPTION_KEY'),
                        `[${translateService.instant(simpleDialogData.primaryButton.labelKey)}]`,
                    ]).toEqual([
                        'Delete the instance?',
                        'This is irreversible. The executable files and output history of this instance will be deleted forever.',
                        '[Delete]',
                    ]);
                    expect(simpleDialogData.extraButtons).toBeUndefined();
                });

                it('should open the confirmation dialog as a warning', () => {
                    expect(simpleDialogData.warn).toBeTrue();
                });

                it('should request to delete the instance', () => {
                    expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'DELETE' });
                });

                it('should update the instance', () => {
                    expect(component.instance).toBe(newInstance);
                });
            });

            describe('when the confirmation dialog is cancelled', () => {
                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.resolveTo(undefined);
                    getButton('Delete').click();
                    tick();
                }));

                it('should keep the instance', () => {
                    expect(component.instance).toBe(instance);
                });
            });

            describe('when the component is already loading', () => {
                let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

                beforeEach(fakeAsync(() => {
                    component.loading = true;
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    getButton('Delete').click();
                    tick();
                }));

                it('should show an error', () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith('Already loading!');
                });
            });
        });
    });

    describe('when the instance is in the VALID state', () => {
        beforeEach(() => {
            instance.state = 'VALID';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance has passed validation.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Delete']);
        });
    });

    describe('when the instance is in the INVALID state', () => {
        beforeEach(() => {
            instance.state = 'INVALID';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance has been marked as invalid!');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Delete']);
        });
    });

    describe('when the instance is in the BROKEN state', () => {
        beforeEach(() => {
            instance.state = 'BROKEN';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is broken! Its actual state is unknown.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Delete']);
        });
    });

    describe('when the instance is in the IDLE state', () => {
        beforeEach(() => {
            instance.state = 'IDLE';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is ready to be started.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Boot up', 'Delete']);
        });

        describe('when the "Boot up" button is clicked', () => {
            let requestActionSpy: jasmine.Spy<RequestActionFn>;
            const newInstance = {} as TerrariaInstanceEntity;

            beforeEach(fakeAsync(() => {
                requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                getButton('Boot up').click();
                tick();
            }));

            it('should request to boot up the instance', () => {
                expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'BOOT_UP' });
            });

            it('should update the instance', () => {
                expect(component.instance).toBe(newInstance);
            });
        });
    });

    describe('when the instance is in the BOOTING_UP state', () => {
        beforeEach(() => {
            instance.state = 'BOOTING_UP';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is booting up.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });

        describe('when the "Terminate" button is clicked', () => {
            describe('when the confirmation dialog is confirmed', () => {
                let simpleDialogData: SimpleDialogInput<TerrariaInstanceEntity>;
                let requestActionSpy: jasmine.Spy<RequestActionFn>;
                const newInstance = {} as TerrariaInstanceEntity;

                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.callFake(async <T>(data: SimpleDialogInput<T>) => {
                        simpleDialogData = data as SimpleDialogInput<any>;
                        return await data.primaryButton.onClicked();
                    });
                    requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                    getButton('Terminate').click();
                    tick();
                }));

                it('should open the confirmation dialog with the correct data', () => {
                    expect([
                        translateService.instant(simpleDialogData.titleKey),
                        translateService.instant(simpleDialogData.descriptionKey || 'NO_DESCRIPTION_KEY'),
                        `[${translateService.instant(simpleDialogData.primaryButton.labelKey)}]`,
                    ]).toEqual([
                        'Terminate the instance?',
                        'This will force the instance ot stop. You may break something this way so you should use this only as a last resort.',
                        '[Terminate]',
                    ]);
                    expect(simpleDialogData.extraButtons).toBeUndefined();
                });

                it('should open the confirmation dialog as a warning', () => {
                    expect(simpleDialogData.warn).toBeTrue();
                });

                it('should request to terminate the instance', () => {
                    expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'TERMINATE' });
                });

                it('should update the instance', () => {
                    expect(component.instance).toBe(newInstance);
                });
            });

            describe('when the confirmation dialog is cancelled', () => {
                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.resolveTo(undefined);
                    getButton('Terminate').click();
                    tick();
                }));

                it('should keep the instance', () => {
                    expect(component.instance).toBe(instance);
                });
            });
        });
    });

    describe('when the instance is in the WORLD_MENU state', () => {
        beforeEach(() => {
            instance.state = 'WORLD_MENU';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the World menu.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Go to the Mods menu', 'Run', 'Shut down', 'Terminate']);
        });

        describe('when the "Go to the Mods menu" button is clicked', () => {
            let requestActionSpy: jasmine.Spy<RequestActionFn>;
            const newInstance = {} as TerrariaInstanceEntity;

            beforeEach(fakeAsync(() => {
                requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                getButton('Go to the Mods menu').click();
                tick();
            }));

            it('should request to go to the Mods menu', () => {
                expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'GO_TO_MOD_MENU' });
            });

            it('should update the instance', () => {
                expect(component.instance).toBe(newInstance);
            });
        });

        describe('when the "Run" button is clicked', () => {
            describe('when the Run Instance dialog is confirmed', () => {
                let runServerSpy: jasmine.Spy<(data: RunServerDialogInput) => Promise<TerrariaInstanceEntity>>;
                const newInstance = {} as TerrariaInstanceEntity;

                beforeEach(fakeAsync(() => {
                    runServerSpy = spyOn(runServerDialogService, 'openDialog').and.resolveTo(newInstance);
                    getButton('Run').click();
                    tick();
                }));

                it('should open the Run Instance dialog with the correct data', () => {
                    expect(runServerSpy).toHaveBeenCalledWith({ hostId: 10, instance });
                });

                it('should update the instance', () => {
                    expect(component.instance).toBe(newInstance);
                });
            });

            describe('when the Run Instance dialog is cancelled', () => {
                beforeEach(fakeAsync(() => {
                    spyOn(runServerDialogService, 'openDialog').and.resolveTo(undefined);
                    getButton('Run').click();
                    tick();
                }));

                it('should keep the instance', () => {
                    expect(component.instance).toBe(instance);
                });
            });

            describe('when the instance is undefined', () => {
                let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

                beforeEach(fakeAsync(() => {
                    component.instance = undefined;
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    getButton('Run').click();
                    tick();
                }));

                it('should show an error', () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith('The instance is not defined!');
                });
            });

            describe('when the host is undefined', () => {
                let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

                beforeEach(fakeAsync(() => {
                    component.host = undefined;
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    getButton('Run').click();
                    tick();
                }));

                it('should show an error', () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith('The host is not defined!');
                });
            });
        });

        describe('when the "Shut down" button is clicked', () => {
            let requestActionSpy: jasmine.Spy<RequestActionFn>;
            const newInstance = {} as TerrariaInstanceEntity;

            beforeEach(fakeAsync(() => {
                requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                getButton('Shut down').click();
                tick();
            }));

            it('should request to shut down the instance', () => {
                expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'SHUT_DOWN' });
            });

            it('should update the instance', () => {
                expect(component.instance).toBe(newInstance);
            });
        });
    });

    describe('when the instance is in the MOD_MENU state', () => {
        beforeEach(() => {
            instance.state = 'MOD_MENU';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the Mods menu.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Set the enabled mods', 'Shut down', 'Terminate']);
        });

        describe('when the "Set the enabled mods" button is clicked', () => {
            describe('when the Set Mods dialog is confirmed', () => {
                let setModsSpy: jasmine.Spy<(data: SetInstanceModsDialogInput) => Promise<TerrariaInstanceEntity>>;
                const newInstance = {} as TerrariaInstanceEntity;

                beforeEach(fakeAsync(() => {
                    setModsSpy = spyOn(setInstanceModsDialogService, 'openDialog').and.resolveTo(newInstance);
                    getButton('Set the enabled mods').click();
                    tick();
                }));

                it('should open the Set Mods dialog with the correct data', () => {
                    expect(setModsSpy).toHaveBeenCalledWith(instance);
                });

                it('should update the instance', () => {
                    expect(component.instance).toBe(newInstance);
                });
            });

            describe('when the Set the Enabled Mods dialog is cancelled', () => {
                beforeEach(fakeAsync(() => {
                    spyOn(setInstanceModsDialogService, 'openDialog').and.resolveTo(undefined);
                    getButton('Set the enabled mods').click();
                    tick();
                }));

                it('should keep the instance', () => {
                    expect(component.instance).toBe(instance);
                });
            });

            describe('when the instance is undefined', () => {
                let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

                beforeEach(fakeAsync(() => {
                    component.instance = undefined;
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    getButton('Set the enabled mods').click();
                    tick();
                }));

                it('should show an error', () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith('The instance is not defined!');
                });
            });
        });
    });

    describe('when the instance is in the CHANGING_MOD_STATE state', () => {
        beforeEach(() => {
            instance.state = 'CHANGING_MOD_STATE';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe(
                'The instance is changing the state of a mod and waiting for the Mods menu to refresh.'
            );
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });
    });

    describe('when the instance is in the MOD_BROWSER state', () => {
        beforeEach(() => {
            instance.state = 'MOD_BROWSER';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the Mod Browser menu.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });
    });

    describe('when the instance is in the MAX_PLAYERS_PROMPT state', () => {
        beforeEach(() => {
            instance.state = 'MAX_PLAYERS_PROMPT';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the max players prompt.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });
    });

    describe('when the instance is in the PORT_PROMPT state', () => {
        beforeEach(() => {
            instance.state = 'PORT_PROMPT';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the port prompt.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });
    });

    describe('when the instance is in the AUTOMATICALLY_FORWARD_PORT_PROMPT state', () => {
        beforeEach(() => {
            instance.state = 'AUTOMATICALLY_FORWARD_PORT_PROMPT';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the automatically forward port prompt.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });
    });

    describe('when the instance is in the PASSWORD_PROMPT state', () => {
        beforeEach(() => {
            instance.state = 'PASSWORD_PROMPT';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is at the password prompt.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });
    });

    describe('when the instance is in the RUNNING state', () => {
        beforeEach(() => {
            instance.state = 'RUNNING';
            initializeWithRoute();
        });

        it('should have the correct info text', () => {
            expect(getInstanceInfoText()).toBe('The instance is running and connecting to it should be possible.');
        });

        it('should have the correct buttons', () => {
            expect(getButtonLabels()).toEqual(['Shut down', 'Terminate']);
        });

        describe('when the "Shut down" button is clicked', () => {
            describe('when the confirmation dialog is confirmed', () => {
                let simpleDialogData: SimpleDialogInput<TerrariaInstanceEntity>;
                let requestActionSpy: jasmine.Spy<RequestActionFn>;
                const newInstance = {} as TerrariaInstanceEntity;

                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.callFake(async <T>(data: SimpleDialogInput<T>) => {
                        simpleDialogData = data as SimpleDialogInput<any>;
                        return await data.primaryButton.onClicked();
                    });
                    requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                    getButton('Shut down').click();
                    tick();
                }));

                it('should open the confirmation dialog with the correct data', () => {
                    const extraButtons = simpleDialogData.extraButtons;
                    expect([
                        translateService.instant(simpleDialogData.titleKey),
                        translateService.instant(simpleDialogData.descriptionKey || 'NO_DESCRIPTION_KEY'),
                        `[${translateService.instant(extraButtons ? extraButtons[0].labelKey : 'NO_EXTRA_BUTTONS')}]`,
                        `[${translateService.instant(simpleDialogData.primaryButton.labelKey)}]`,
                    ]).toEqual([
                        'Shut the instance down?',
                        'A server is running through this instance right now. ' +
                            'All players will be disconnected. ' +
                            'Would you like to save the state of the world, or lose all changes to it?',
                        '[Shut down]',
                        '[Save and shut down]',
                    ]);
                    expect(simpleDialogData.extraButtons?.length).toBe(1);
                });

                it('should not open the confirmation dialog as a warning', () => {
                    expect(simpleDialogData.warn).toBeUndefined();
                });

                it('should request to delete the instance', () => {
                    expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'SHUT_DOWN' });
                });

                it('should update the instance', () => {
                    expect(component.instance).toBe(newInstance);
                });
            });

            describe('when the extra button (shut down without saving) is clicked', () => {
                let requestActionSpy: jasmine.Spy<RequestActionFn>;
                const newInstance = {} as TerrariaInstanceEntity;

                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.callFake(async <T>(data: SimpleDialogInput<T>) => {
                        const extraButton = data.extraButtons ? data.extraButtons[0] : undefined;
                        if (!extraButton) {
                            throw new Error('There is no extra button!');
                        }
                        return await extraButton.onClicked();
                    });
                    requestActionSpy = spyOn(restApiService, 'requestActionForInstance').and.resolveTo(newInstance);
                    getButton('Shut down').click();
                    tick();
                }));

                it('should request to delete the instance', () => {
                    expect(requestActionSpy).toHaveBeenCalledOnceWith(20, { action: 'SHUT_DOWN_NO_SAVE' });
                });

                it('should update the instance', () => {
                    expect(component.instance).toBe(newInstance);
                });
            });

            describe('when the confirmation dialog is cancelled', () => {
                beforeEach(fakeAsync(() => {
                    spyOn(simpleDialogService, 'openDialog').and.resolveTo(undefined);
                    getButton('Shut down').click();
                    tick();
                }));

                it('should keep the instance', () => {
                    expect(component.instance).toBe(instance);
                });
            });

            describe('when the instance is undefined', () => {
                let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

                beforeEach(fakeAsync(() => {
                    component.instance = undefined;
                    showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                    getButton('Shut down').click();
                    tick();
                }));

                it('should show an error', () => {
                    expect(showErrorSpy).toHaveBeenCalledOnceWith('The instance is not defined!');
                });
            });
        });
    });
});
