import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { Subject } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import { HostEntity, TerrariaInstanceEntity, TerrariaInstanceUpdateModel } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
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
            ],
        }).compileComponents();

        authenticationService = TestBed.inject(AuthenticationService);
        userSubject = new Subject();
        spyOnProperty(authenticationService, 'userObservable', 'get').and.returnValue(userSubject.asObservable());

        restApiService = TestBed.inject(RestApiService);

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

    describe('when there is no instance', () => {
        beforeEach(fakeAsync(() => {
            component.instance = undefined;
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

            beforeEach(() => {
                updateSpy = spyOn(restApiService, 'updateInstance');
            });

            it('should not do anything', async () => {
                await component.renameInstance();
                expect(updateSpy).not.toHaveBeenCalled();
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
                component.renaming = true;
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

    describe('when renaming', () => {
        beforeEach(() => (component.renaming = true));

        describe('onRenameCancelled', () => {
            beforeEach(() => component.onRenameCancelled());

            it('should stop the renaming', () => {
                expect(component.renaming).toBeFalse();
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
