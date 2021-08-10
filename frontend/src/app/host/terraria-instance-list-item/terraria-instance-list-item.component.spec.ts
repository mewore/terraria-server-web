import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable, Subject } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { MessageService } from 'src/app/core/services/message.service';
import { MessageServiceStub } from 'src/app/core/services/message.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import { TerrariaInstanceService } from 'src/app/terraria-core/services/terraria-instance.service';
import { TerrariaInstanceServiceStub } from 'src/app/terraria-core/services/terraria-instance.service.stub';
import { HostEntity, TerrariaInstanceEntity, TerrariaInstanceUpdateModel } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
import { MatFormFieldInfo } from 'src/test-util/form-field-info';
import { ListItemInfo, MaterialListItemInfo } from 'src/test-util/list-item-info';
import { TerrariaInstanceListItemComponent } from './terraria-instance-list-item.component';

describe('TerrariaInstanceListItemComponent', () => {
    let fixture: ComponentFixture<TerrariaInstanceListItemComponent>;
    let component: TerrariaInstanceListItemComponent;
    let listItemInfo: ListItemInfo;

    let host: HostEntity;
    let instance: TerrariaInstanceEntity;

    let userSubject: Subject<AuthenticatedUser>;
    let authenticationService: AuthenticationService;

    let restApiService: RestApiService;
    let terrariaInstanceService: TerrariaInstanceService;
    let messageService: MessageService;
    let errorService: ErrorService;

    let instanceDeletionSubject: Subject<void>;
    let watchInstanceDeletionSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => Observable<void>>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                ReactiveFormsModule,
                MatInputModule,
                MatListModule,
                MatIconModule,
                MatTooltipModule,
                MatProgressSpinnerModule,
                NoopAnimationsModule,
                RouterTestingModule,
            ],
            declarations: [TerrariaInstanceListItemComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: TerrariaInstanceService, useClass: TerrariaInstanceServiceStub },
                { provide: MessageService, useClass: MessageServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
            ],
        }).compileComponents();

        authenticationService = TestBed.inject(AuthenticationService);
        userSubject = new Subject();
        spyOnProperty(authenticationService, 'userObservable', 'get').and.returnValue(userSubject.asObservable());

        restApiService = TestBed.inject(RestApiService);
        terrariaInstanceService = TestBed.inject(TerrariaInstanceService);
        messageService = TestBed.inject(MessageService);
        errorService = TestBed.inject(ErrorService);

        [fixture, component] = await initComponent(TerrariaInstanceListItemComponent);
        listItemInfo = new MaterialListItemInfo(fixture);

        host = {
            id: 1,
        } as HostEntity;
        component.host = host;

        instance = {
            id: 2,
            name: 'Test Instance',
            modLoaderVersion: '1.0.0',
            modLoaderReleaseUrl: 'mod-loader-release-url',
            modLoaderArchiveUrl: 'mod-loader-archive-url',
            terrariaVersion: '1.2.3',
            terrariaServerUrl: 'server-url',
        } as TerrariaInstanceEntity;

        instanceDeletionSubject = new Subject();
        watchInstanceDeletionSpy = spyOn(messageService, 'watchInstanceDeletion').and.returnValue(
            instanceDeletionSubject.asObservable()
        );
        component.instance = instance;
        fakeAsync(() => refreshFixture(fixture))();
    });

    const getInput = (): HTMLInputElement | undefined =>
        (fixture.nativeElement as HTMLElement).querySelector('input') || undefined;

    it('should have the correct lines', () => {
        expect(listItemInfo.lines).toEqual([
            'Test Instance',
            'TModLoader: 1.0.0 (Download)',
            'Terraria server: 1.2.3 (Download)',
        ]);
    });

    it('should have the correct buttons', () => {
        expect(listItemInfo.buttonLabels).toEqual(['delete', 'edit']);
    });

    it('should not have an input', () => {
        expect(getInput()).toBeUndefined();
    });

    it('should watch for the deletion of the instance', () => {
        expect(watchInstanceDeletionSpy).toHaveBeenCalledOnceWith(instance);
    });

    describe('the tModLoader version link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(1, '1.0.0')).toBe('mod-loader-release-url');
        });
    });

    describe('the tModLoader download link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(1, '(Download)')).toBe('mod-loader-archive-url');
        });
    });

    describe('the server version link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(2, '1.2.3')).toBe('https://terraria.gamepedia.com/Server#Downloads');
        });
    });

    describe('the server download link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(2, '(Download)')).toBe('server-url');
        });
    });

    describe('when the instance has a pending action', () => {
        beforeEach(fakeAsync(() => {
            instance.pendingAction = 'BOOT_UP';
        }));

        describe('when terrariaInstanceService#canDelete returns true', () => {
            beforeEach(fakeAsync(() => {
                spyOn(terrariaInstanceService, 'canDelete').and.returnValue(true);
                refreshFixture(fixture);
            }));

            describe('canDelete', () => {
                it('should be false', () => {
                    expect(component.canDelete).toBeFalse();
                });
            });
        });
    });

    describe('when there is no instance', () => {
        beforeEach(fakeAsync(() => {
            component.instance = undefined;
            refreshFixture(fixture);
        }));

        describe('the Delete button', () => {
            it('should be disabled', () => {
                expect(listItemInfo.getButton('delete')?.disabled).toBeTrue();
            });
        });

        describe('deleted', () => {
            it('should be false', () => {
                expect(component.deleted).toBeFalse();
            });
        });

        describe('when terrariaInstanceService#canDelete returns true', () => {
            beforeEach(fakeAsync(() => {
                spyOn(terrariaInstanceService, 'canDelete').and.returnValue(true);
                refreshFixture(fixture);
            }));

            describe('canDelete', () => {
                it('should be true', () => {
                    expect(component.canDelete).toBeTrue();
                });
            });
        });

        describe('the Rename button', () => {
            it('should be disabled', () => {
                expect(listItemInfo.getButton('edit')?.disabled).toBeTrue();
            });
        });

        describe('onRenameClicked', () => {
            beforeEach(() => {
                component.onRenameClicked();
            });

            it('should not do anything', () => {
                expect(component.renaming).toBeFalse();
            });
        });

        describe('renameInstance', () => {
            let updateSpy: jasmine.Spy;
            let showErrorSpy: jasmine.Spy<(error: string) => void>;

            beforeEach(async () => {
                component.nameInput.setValue('Some new name');
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                await component.renameInstance();
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith('The instance is not defined!');
            });
        });

        describe('onDeleteClicked', () => {
            let showErrorSpy: jasmine.Spy<(error: string) => void>;

            beforeEach(() => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                component.onDeleteClicked();
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith('The instance is not defined!');
            });
        });
    });

    describe('when the instance is set to another one', () => {
        const newInstance = { id: 5 } as TerrariaInstanceEntity;

        beforeEach(fakeAsync(() => {
            component.instance = newInstance;
            refreshFixture(fixture);
        }));

        it('should watch for the deletion of the new instance', () => {
            expect(watchInstanceDeletionSpy).toHaveBeenCalledWith(newInstance);
        });
    });

    describe('when the instance is deleted', () => {
        beforeEach(fakeAsync(() => {
            instanceDeletionSubject.next();
            refreshFixture(fixture);
        }));

        describe('deleted', () => {
            it('should be true', () => {
                expect(component.deleted).toBeTrue();
            });
        });

        describe('the instance name line', () => {
            it('should end with "(DELETED)"', () => {
                expect(listItemInfo.lines[0]).toMatch(/ \(DELETED\)$/);
            });
        });

        describe('when the instance is set to another one with the same ID', () => {
            beforeEach(fakeAsync(() => {
                component.instance = { id: instance.id } as TerrariaInstanceEntity;
                refreshFixture(fixture);
            }));

            describe('deleted', () => {
                it('should still be true', () => {
                    expect(component.deleted).toBeTrue();
                });
            });
        });

        describe('when the instance is set to another one with a different ID', () => {
            beforeEach(fakeAsync(() => {
                component.instance = { id: instance.id + 1 } as TerrariaInstanceEntity;
                refreshFixture(fixture);
            }));

            describe('deleted', () => {
                it('should be false', () => {
                    expect(component.deleted).toBeFalse();
                });
            });
        });
    });

    describe('when not allowed to manage Terraria instances', () => {
        beforeEach(fakeAsync(() => {
            spyOnProperty(authenticationService, 'canManageTerraria', 'get').and.returnValue(false);
            userSubject.next(undefined);
            refreshFixture(fixture);
        }));

        describe('the Rename button', () => {
            it('should be disabled', () => {
                expect(listItemInfo.getButton('edit')?.disabled).toBeTrue();
            });
        });

        describe('onRenameClicked', () => {
            beforeEach(() => {
                component.onRenameClicked();
            });

            it('should not do anything', () => {
                expect(component.renaming).toBeFalse();
            });
        });

        describe('renameInstance', () => {
            let updateSpy: jasmine.Spy;

            beforeEach(async () => {
                component.nameInput.setValue('Something Different');
                updateSpy = spyOn(restApiService, 'updateInstance');
                await component.renameInstance();
            });

            it('should not do anything', () => {
                expect(updateSpy).not.toHaveBeenCalled();
            });
        });

        describe('when renaming', () => {
            beforeEach(fakeAsync(() => {
                component.action = 'RENAME';
                refreshFixture(fixture);
            }));

            describe('onRenameConfirmed', () => {
                let updateSpy: jasmine.Spy;

                beforeEach(async () => {
                    await component.renameInstance();
                    updateSpy = spyOn(restApiService, 'updateInstance');
                });

                it('should not update', () => {
                    expect(updateSpy).not.toHaveBeenCalled();
                });
            });
        });
    });

    describe('while loading', () => {
        beforeEach(() => {
            component.loading = true;
        });

        describe('onRenameConfirmed', () => {
            let eventMock: Event;

            beforeEach(() => {
                eventMock = jasmine.createSpyObj('event', ['stopPropagation', 'preventDefault']);
            });

            it('should not do anything', async () => {
                component.onRenameConfirmed(eventMock);
                expect(eventMock.stopPropagation).not.toHaveBeenCalled();
                expect(eventMock.preventDefault).not.toHaveBeenCalled();
            });
        });

        describe('onDeleteClicked', () => {
            let showErrorSpy: jasmine.Spy<(error: string) => void>;

            beforeEach(() => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                component.onDeleteClicked();
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith('Already loading!');
            });
        });
    });

    describe('focusOnNameInput', () => {
        describe('when there is no nameInputElement', () => {
            beforeEach(() => (component.nameInputElement = undefined));

            it('should not do anything', () => {
                component.focusOnNameInput();
                expect(component).toBeTruthy();
            });
        });

        describe('when there is nameInputElement', () => {
            let focusSpy: jasmine.Spy<() => void>;

            beforeEach(() => {
                component.nameInputElement = { nativeElement: { focus: () => {} } as HTMLInputElement };
                focusSpy = spyOn(component.nameInputElement.nativeElement, 'focus');
                component.focusOnNameInput();
            });

            it('should focus onto the input', () => {
                expect(focusSpy).toHaveBeenCalledOnceWith();
            });
        });
    });

    describe('when the Delete button is clicked', () => {
        let deleteSpy: jasmine.Spy<(instance?: TerrariaInstanceEntity) => Promise<TerrariaInstanceEntity | undefined>>;
        let oldInstance: TerrariaInstanceEntity | undefined;

        describe('when the deletion returns undefined', () => {
            beforeEach(() => {
                oldInstance = component.instance;
                deleteSpy = spyOn(terrariaInstanceService, 'delete').and.resolveTo(undefined);
                listItemInfo.clickButton('delete');
            });

            it('should keep the instance', () => {
                expect(component.instance).toBe(oldInstance);
            });
        });

        describe('when the deletion is confirmed', () => {
            const returnedInstance = {} as TerrariaInstanceEntity;

            beforeEach(() => {
                oldInstance = component.instance;
                deleteSpy = spyOn(terrariaInstanceService, 'delete').and.resolveTo(returnedInstance);
                listItemInfo.clickButton('delete');
            });

            it('should request to delete the instance', () => {
                expect(deleteSpy).toHaveBeenCalledOnceWith(oldInstance);
            });

            it('should update the instance', () => {
                expect(component.instance).toBe(returnedInstance);
            });

            // The instance is not really deleted - only an action for its future deletion is requested
            it('should not mark the instance as deleted yet', () => {
                expect(component.deleted).toBeFalse();
            });
        });
    });

    describe('when renaming', () => {
        beforeEach(() => {
            component.action = 'RENAME';
            component.nameInput.setValue('New name');
        });

        describe('onRenameCancelled', () => {
            beforeEach(() => component.onRenameCancelled());

            it('should stop the renaming', () => {
                expect(component.renaming).toBeFalse();
            });
        });

        describe('when the instance is deleted', () => {
            beforeEach(fakeAsync(() => {
                instanceDeletionSubject.next();
                refreshFixture(fixture);
            }));

            describe('the name input', () => {
                it('should have an error', () => {
                    expect(new MatFormFieldInfo(fixture, 0, component.nameInput).errors).toEqual([
                        'The instance has been deleted',
                    ]);
                });
            });

            describe('renameInstance', () => {
                let updateSpy: jasmine.Spy;

                beforeEach(() => {
                    updateSpy = spyOn(restApiService, 'updateInstance');
                    return component.renameInstance();
                });

                it('should not rename', () => {
                    expect(updateSpy).not.toHaveBeenCalled();
                });
            });
        });
    });

    describe('when the Rename button is clicked', () => {
        beforeEach(() => listItemInfo.clickButton('edit'));

        describe('the name input', () => {
            it('should exist', () => {
                expect(getInput()).toBeDefined();
            });

            it('should have the instance name as a placeholder and value', () => {
                expect(getInput()?.placeholder).toBe('Test Instance');
                expect(getInput()?.value).toBe('Test Instance');
            });
        });

        describe('the Rename button', () => {
            it('should now be a Confirm button', () => {
                expect(listItemInfo.buttonLabels).toEqual(['delete', 'check']);
            });
        });

        describe('when the name is changed', () => {
            beforeEach(fakeAsync(() => {
                component.nameInput.setValue('New Instance Name');
                refreshFixture(fixture);
            }));

            describe('when the Confirm Rename button is clicked', () => {
                let updatedInstance: TerrariaInstanceEntity;
                let updateSpy: jasmine.Spy<
                    (instanceId: number, model: TerrariaInstanceUpdateModel) => Promise<TerrariaInstanceEntity>
                >;

                beforeEach(() => {
                    updatedInstance = {} as TerrariaInstanceEntity;
                    updateSpy = spyOn(restApiService, 'updateInstance').and.resolveTo(updatedInstance);
                    listItemInfo.clickButton('check');
                });

                it('should not have an input anymore', () => {
                    expect(getInput()).toBeUndefined();
                });

                it('should update the instance', () => {
                    expect(updateSpy).toHaveBeenCalledOnceWith(2, { newName: 'New Instance Name' });
                });

                it('should use the received instance', () => {
                    expect(component.instance).toBe(updatedInstance);
                });
            });
        });

        describe('when the Confirm Rename button is clicked', () => {
            let updateSpy: jasmine.Spy;

            beforeEach(() => {
                updateSpy = spyOn(restApiService, 'updateInstance');
                listItemInfo.clickButton('check');
            });

            it('should not have an input anymore', () => {
                expect(getInput()).toBeUndefined();
            });
        });
    });
});
