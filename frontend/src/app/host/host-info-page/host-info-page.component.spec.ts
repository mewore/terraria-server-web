import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Subject } from 'rxjs';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { MessageService } from 'src/app/core/services/message.service';
import { MessageServiceStub } from 'src/app/core/services/message.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { HostEntity, TerrariaInstanceEntity, TerrariaWorldEntity } from 'src/generated/backend';
import { FakeParamMap } from 'src/stubs/fake-param-map';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
import { CreateTerrariaInstanceDialogService } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service';
import { CreateTerrariaInstanceDialogServiceStub } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service.stub';
import { HostListItemStubComponent } from '../host-list-item/host-list-item.component.stub';
import { NewItemButtonStubComponent } from '../new-item-button/new-item-button.component.stub';
import { TerrariaInstanceListItemStubComponent } from '../terraria-instance-list-item/terraria-instance-list-item.component.stub';
import { TerrariaWorldListItemStubComponent } from '../terraria-world-list-item/terraria-world-list-item.component.stub';

import { HostInfoPageComponent } from './host-info-page.component';

describe('HostInfoComponent', () => {
    let component: HostInfoPageComponent;
    let fixture: ComponentFixture<HostInfoPageComponent>;

    let restApiService: RestApiService;
    let routeSubject: Subject<ParamMap>;
    let dialogService: CreateTerrariaInstanceDialogService;

    let messageService: MessageService;
    let instanceCreationSubject: Subject<TerrariaInstanceEntity>;

    let errorService: ErrorService;

    beforeEach(async () => {
        routeSubject = new Subject();

        await TestBed.configureTestingModule({
            imports: [MatProgressSpinnerModule, MatListModule],
            declarations: [
                HostInfoPageComponent,
                HostListItemStubComponent,
                TerrariaInstanceListItemStubComponent,
                TerrariaWorldListItemStubComponent,
                NewItemButtonStubComponent,
            ],
            providers: [
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: ActivatedRoute, useValue: { paramMap: routeSubject.asObservable() } },
                { provide: CreateTerrariaInstanceDialogService, useClass: CreateTerrariaInstanceDialogServiceStub },
                { provide: MessageService, useClass: MessageServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
            ],
        }).compileComponents();

        restApiService = TestBed.inject(RestApiService);
        dialogService = TestBed.inject(CreateTerrariaInstanceDialogService);

        messageService = TestBed.inject(MessageService);
        instanceCreationSubject = new Subject();
        spyOn(messageService, 'watchHostInstanceCreation').and.returnValue(instanceCreationSubject.asObservable());

        errorService = TestBed.inject(ErrorService);

        [fixture, component] = await initComponent(HostInfoPageComponent);
    });

    afterEach(() => {
        fixture.destroy();
    });

    it('should not be loading', () => {
        expect(component.loading).toBeFalse();
    });

    it('should be loaded', () => {
        expect(component.loaded).toBeTrue();
    });

    describe('when there is no subscription', () => {
        beforeEach(() => {
            component.routeSubscription?.unsubscribe();
            component.routeSubscription = undefined;
        });

        describe('ngOnDestroy', () => {
            it('should not throw an error', () => {
                expect(() => component.ngOnDestroy()).not.toThrow();
            });
        });
    });

    describe('when there is no host info', () => {
        describe('when the creation of a terraria instance has been requested', () => {
            let openDialogSpy: jasmine.Spy<(host: HostEntity) => void>;
            let showErrorSpy: jasmine.Spy<(error: Error) => void>;

            beforeEach(async () => {
                openDialogSpy = spyOn(dialogService, 'openDialog');
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                await component.terrariaInstanceCreationRequested();
            });

            it('should not open a dialog', () => {
                expect(openDialogSpy).not.toHaveBeenCalled();
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(
                    new Error('The data has not been loaded. Cannot create a Terraria instance.')
                );
            });
        });
    });

    describe('when there is a new activated route', () => {
        describe('when the route is invalid', () => {
            let showErrorSpy: jasmine.Spy<(error: Error) => void>;

            beforeEach(fakeAsync(() => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                routeSubject.next(new FakeParamMap());
                flushMicrotasks();
            }));

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(new Error('The [hostId] parameter is not set!'));
            });
        });

        describe('when the route is valid', () => {
            let host: HostEntity;
            let getHostSpy: jasmine.Spy<(hostId: number) => Promise<HostEntity>>;
            let getInstancesSpy: jasmine.Spy<(hostId: number) => Promise<TerrariaInstanceEntity[]>>;
            let getWorldsSpy: jasmine.Spy<(hostId: number) => Promise<TerrariaWorldEntity[]>>;

            beforeEach(fakeAsync(() => {
                host = { id: 10 } as HostEntity;
                getHostSpy = spyOn(restApiService, 'getHost').and.resolveTo(host);
                getInstancesSpy = spyOn(restApiService, 'getHostInstances').and.resolveTo([
                    { id: 2 } as TerrariaInstanceEntity,
                ]);
                getWorldsSpy = spyOn(restApiService, 'getHostWorlds').and.resolveTo([{ id: 3 } as TerrariaWorldEntity]);

                routeSubject.next(new FakeParamMap({ hostId: '10' }));
                flushMicrotasks();
            }));

            it('should fetch the host data', () => {
                expect(getHostSpy).toHaveBeenCalledOnceWith(10);
                expect(getInstancesSpy).toHaveBeenCalledOnceWith(10);
                expect(getWorldsSpy).toHaveBeenCalledOnceWith(10);
            });

            it('should set the host', () => {
                expect(component.host?.id).toBe(10);
            });

            it('should set the instances', () => {
                expect(component.instances).toEqual([{ id: 2 } as TerrariaInstanceEntity]);
            });

            it('should set the worlds', () => {
                expect(component.worlds).toEqual([{ id: 3 } as TerrariaWorldEntity]);
            });

            describe('when there is another valid host route', () => {
                beforeEach(fakeAsync(() => {
                    routeSubject.next(new FakeParamMap({ hostId: '10' }));
                    getInstancesSpy.and.resolveTo([{ id: 8 } as TerrariaInstanceEntity]);
                    refreshFixture(fixture);
                }));

                it('should set the instances again', () => {
                    instanceCreationSubject.next({ id: 8 } as TerrariaInstanceEntity);
                });
            });

            describe('when there is a message for the creation of a new instance', () => {
                beforeEach(fakeAsync(() => {
                    instanceCreationSubject.next({ id: 8 } as TerrariaInstanceEntity);
                }));

                it('should save the new instance', () => {
                    expect(component.instances).toEqual([
                        { id: 2 } as TerrariaInstanceEntity,
                        { id: 8 } as TerrariaInstanceEntity,
                    ]);
                });
            });

            describe('when there is a message for the creation of an already present instance', () => {
                beforeEach(fakeAsync(() => {
                    instanceCreationSubject.next({ id: 2, state: 'IDLE' } as TerrariaInstanceEntity);
                }));

                it('should save the replace the known instance with the new one', () => {
                    expect(component.instances).toEqual([{ id: 2, state: 'IDLE' } as TerrariaInstanceEntity]);
                });
            });

            describe('when the creation of a terraria instance has been requested', () => {
                let openDialogSpy: jasmine.Spy<(host: HostEntity) => Promise<TerrariaInstanceEntity | undefined>>;

                beforeEach(async () => {
                    openDialogSpy = spyOn(dialogService, 'openDialog');
                });

                it('should open a dialog with the host', async () => {
                    openDialogSpy = openDialogSpy.and.resolveTo(undefined);
                    await component.terrariaInstanceCreationRequested();
                    expect(openDialogSpy).toHaveBeenCalledWith(host);
                });

                describe('when the creation is cancelled', () => {
                    beforeEach(async () => {
                        openDialogSpy = openDialogSpy.and.resolveTo(undefined);
                        await component.terrariaInstanceCreationRequested();
                    });

                    it('should do nothing', () => {
                        expect(component.instances).toEqual([{ id: 2 } as TerrariaInstanceEntity]);
                    });
                });

                describe('when the creation is successful', () => {
                    beforeEach(async () => {
                        openDialogSpy = openDialogSpy.and.resolveTo({ id: 8 } as TerrariaInstanceEntity);
                        await component.terrariaInstanceCreationRequested();
                    });

                    it('should save the new instance', () => {
                        expect(component.instances).toEqual([
                            { id: 2 } as TerrariaInstanceEntity,
                            { id: 8 } as TerrariaInstanceEntity,
                        ]);
                    });
                });
            });
        });
    });
});
