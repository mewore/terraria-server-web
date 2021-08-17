import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { TerrariaInstanceEntity, TerrariaInstanceUpdateModel, TerrariaWorldEntity } from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo } from 'src/test-util/dialog-info';
import { FormFieldInfo, MatFormFieldInfo } from 'src/test-util/form-field-info';
import { RunServerDialogComponent, RunServerDialogInput, RunServerDialogOutput } from './run-server-dialog.component';

describe('RunServerDialogComponent', () => {
    let fixture: ComponentFixture<RunServerDialogComponent>;
    let component: RunServerDialogComponent;
    let dialog: DialogInfo;

    let dialogRef: MatDialogRef<RunServerDialogComponent, RunServerDialogOutput>;
    let restApiService: RestApiService;
    let instance: TerrariaInstanceEntity;
    let data: RunServerDialogInput;

    let getHostWorldsSpy: jasmine.Spy<(hostId: number) => Promise<TerrariaWorldEntity[]>>;
    let hostWorlds: TerrariaWorldEntity[];
    const worldWithNoModsId = 11;
    const worldWithRunningHostId = 12;
    const worldWithMatchingModsId = 13;
    const worldWithMismatchingModsId = 14;
    const missingWorldId = 15;

    let getHostInstancesSpy: jasmine.Spy<(hostId: number) => Promise<TerrariaInstanceEntity[]>>;
    let hostInstances: TerrariaInstanceEntity[];

    const init = async (): Promise<void> => {
        [fixture, component] = await initComponent(RunServerDialogComponent);
        dialog = new MaterialDialogInfo(fixture);
    };

    beforeEach(async () => {
        instance = {
            id: 2,
            loadedMods: ['Mod'],
            port: 7000,
            state: 'WORLD_MENU',
        } as TerrariaInstanceEntity;
        data = { hostId: 1, instance };

        hostWorlds = [];

        hostInstances = [
            instance,
            {
                state: 'IDLE',
                port: 7770,
                worldId: undefined,
            } as TerrariaInstanceEntity,
            {
                state: 'IDLE',
                port: 7771,
                worldId: worldWithMatchingModsId,
            } as TerrariaInstanceEntity,
            {
                state: 'RUNNING',
                port: 7772,
                worldId: worldWithRunningHostId,
            } as TerrariaInstanceEntity,
            {
                state: 'RUNNING',
                port: 7771,
                worldId: worldWithRunningHostId,
            } as TerrariaInstanceEntity,
            {
                state: 'RUNNING',
                port: 7771,
            } as TerrariaInstanceEntity,
        ];

        await TestBed.configureTestingModule({
            imports: [
                MatDialogModule,
                ReactiveFormsModule,
                MatInputModule,
                MatSelectModule,
                MatCheckboxModule,
                MatProgressBarModule,
                MatIconModule,
                MatTooltipModule,
                NoopAnimationsModule,
            ],
            declarations: [RunServerDialogComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: MAT_DIALOG_DATA, useValue: data },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);

        restApiService = TestBed.inject(RestApiService);
        getHostWorldsSpy = spyOn(restApiService, 'getHostWorlds').and.callFake(() => Promise.resolve(hostWorlds));
        getHostInstancesSpy = spyOn(restApiService, 'getHostInstances').and.callFake(() =>
            Promise.resolve(hostInstances)
        );
    });

    it('should have the correct title', async () => {
        await init();
        expect(dialog.title).toBe('Run the Terraria instance as a server');
    });

    it('should have the correct buttons', async () => {
        await init();
        expect(dialog.buttons).toEqual(['Cancel', 'Run']);
    });

    describe('when there are worlds', () => {
        beforeEach(async () => {
            hostWorlds = [
                {
                    id: worldWithNoModsId,
                    displayName: 'World1',
                    lastModified: '',
                    mods: undefined,
                } as TerrariaWorldEntity,
                {
                    id: worldWithRunningHostId,
                    displayName: 'World2',
                    lastModified: '',
                    mods: ['Mod'],
                } as TerrariaWorldEntity,
                {
                    id: worldWithMatchingModsId,
                    displayName: 'World3',
                    lastModified: '',
                    mods: ['Mod'],
                } as TerrariaWorldEntity,
                {
                    id: worldWithMismatchingModsId,
                    displayName: 'World4',
                    lastModified: '',
                    mods: ['OtherMod'],
                } as TerrariaWorldEntity,
                {
                    id: missingWorldId,
                    displayName: 'World4',
                    lastModified: undefined,
                    mods: ['OtherMod'],
                } as TerrariaWorldEntity,
            ];
            await init();
        });

        it('should not be loading', () => {
            expect(component.loading).toBeFalse();
        });

        it('should fetch the host worlds', () => {
            expect(getHostWorldsSpy).toHaveBeenCalledOnceWith(1);
        });

        it('should fetch the host instances', () => {
            expect(getHostInstancesSpy).toHaveBeenCalledOnceWith(1);
        });

        describe('the Max Players form field', () => {
            let maxPlayersField: FormFieldInfo;

            beforeEach(() => {
                maxPlayersField = new MatFormFieldInfo(fixture, 0, component.maxPlayersInput);
            });

            it('should have the correct label', () => {
                expect(maxPlayersField.label).toBe('Max players');
            });

            it('should have the correct default value', () => {
                expect(maxPlayersField.value).toBe(8);
            });

            it('should not have any errors', () => {
                expect(maxPlayersField.errors).toEqual([]);
            });

            describe('when nothing is entered', () => {
                beforeEach(() => {
                    maxPlayersField.setValue('');
                });

                it('should have an error', () => {
                    expect(maxPlayersField.errors).toEqual(['* Required']);
                });
            });

            describe('when -1 is entered', () => {
                beforeEach(() => {
                    maxPlayersField.setValue(-1);
                });

                it('should have an error', () => {
                    expect(maxPlayersField.errors).toEqual(['Should be at least 0']);
                });
            });

            describe('when a huge number is entered', () => {
                beforeEach(() => {
                    maxPlayersField.setValue(100000000000);
                });

                it('should have an error', () => {
                    expect(maxPlayersField.errors).toEqual(['Should be at most 1000000000']);
                });
            });
        });

        describe('the Port form field', () => {
            let portField: FormFieldInfo;

            beforeEach(() => {
                portField = new MatFormFieldInfo(fixture, 1, component.portInput);
            });

            it('should have the correct label', () => {
                expect(portField.label).toBe('Port');
            });

            it('should have the correct default value', () => {
                expect(portField.value).toBe(7777);
            });

            it('should not have any errors', () => {
                expect(portField.errors).toEqual([]);
            });

            describe('when nothing is entered', () => {
                beforeEach(() => {
                    portField.setValue('');
                });

                it('should have an error', () => {
                    expect(portField.errors).toEqual(['* Required']);
                });
            });

            describe('when 100 is entered', () => {
                beforeEach(() => {
                    portField.setValue(100);
                });

                it('should have an error', () => {
                    expect(portField.errors).toEqual(['Should be at least 1024']);
                });
            });

            describe('when a 100000 is entered', () => {
                beforeEach(() => {
                    portField.setValue(100000);
                });

                it('should have an error', () => {
                    expect(portField.errors).toEqual(['Should be at most 49151']);
                });
            });

            describe('when a taken port is entered', () => {
                beforeEach(() => {
                    portField.setValue(7772);
                });

                it('should have an error', () => {
                    expect(portField.errors).toEqual(['There is already a server running on port 7772 on this host']);
                });
            });
        });

        describe('the Automatically foward port form field', () => {
            it('should have the correct default value', () => {
                expect(component.automaticallyForwardPortInput.value).toBe(true);
            });
        });

        describe('the Password form field', () => {
            let passwordField: FormFieldInfo;

            beforeEach(() => {
                passwordField = new MatFormFieldInfo(fixture, 2, component.passwordInput);
            });

            it('should have the correct label', () => {
                expect(passwordField.label).toBe('Password');
            });

            it('should have the correct default value', () => {
                expect(passwordField.value).toBe('');
            });

            it('should not have any errors', () => {
                expect(passwordField.errors).toEqual([]);
            });

            describe('when a string with length 256 is entered', () => {
                beforeEach(() => {
                    const characters = [];
                    for (let i = 0; i < 256; i++) {
                        characters.push('a');
                    }
                    passwordField.setValue(characters.join(''));
                });

                it('should have an error', () => {
                    expect(passwordField.errors).toEqual(['Too long (max length: 255)']);
                });
            });
        });

        describe('the World form field', () => {
            let worldField: FormFieldInfo;

            beforeEach(() => {
                worldField = new MatFormFieldInfo(fixture, 3, component.worldInput);
            });

            it('should have the correct label', () => {
                expect(worldField.label).toBe('World');
            });

            it('should have the correct default value', () => {
                expect(worldField.value).toBe(11);
            });

            it('should not have any errors', () => {
                expect(worldField.errors).toEqual([]);
            });

            describe('when a world with matching mods is selected', () => {
                beforeEach(() => {
                    worldField.setValue(worldWithMatchingModsId);
                });

                it('should have no warnings', () => {
                    expect(worldField.warnings).toEqual([]);
                });
            });

            describe('when a world with no known mods is selected', () => {
                beforeEach(() => {
                    worldField.setValue(worldWithNoModsId);
                });

                it('should have a warning', () => {
                    expect(worldField.warnings).toEqual(['The mods of the world are unknown.']);
                });
            });

            describe('when a world with a host running with it is selected', () => {
                beforeEach(() => {
                    worldField.setValue(worldWithRunningHostId);
                });

                it('should have a warning', () => {
                    expect(worldField.warnings).toEqual(['A server is already running with this world.']);
                });
            });

            describe('when a world with mismatching mods is selected', () => {
                beforeEach(() => {
                    worldField.setValue(worldWithMismatchingModsId);
                });

                it('should have a warning', () => {
                    expect(worldField.warnings).toEqual([
                        'The mods of the world are different from the ones of the instance.',
                    ]);
                });
            });

            describe('when no world is selected', () => {
                beforeEach(() => {
                    worldField.setValue(undefined);
                });

                it('should have an error', () => {
                    expect(worldField.errors).toEqual(['* Required']);
                });
            });

            describe('when a missing world (with no timestamp) is selected', () => {
                beforeEach(() => {
                    worldField.setValue(missingWorldId);
                });

                it('should have a warning', () => {
                    expect(worldField.errors).toEqual(['The files of the world are missing']);
                });
            });
        });

        describe('when valid values have been entered', () => {
            beforeEach(fakeAsync(() => {
                component.maxPlayersInput.setValue(1);
                component.portInput.setValue(9999);
                component.automaticallyForwardPortInput.setValue(false);
                component.passwordInput.setValue('test-password');
                component.worldInput.setValue(worldWithMatchingModsId);
                refreshFixture(fixture);
            }));

            it('should have a valid form', () => {
                expect(component.form.valid).toBeTrue();
            });

            it('all buttons should be enabled', () => {
                expect(dialog.enabledButtons).toEqual(['Cancel', 'Run']);
            });

            describe('clicking on Run', () => {
                let updateInstanceSpy: jasmine.Spy<
                    (instanceId: number, model: TerrariaInstanceUpdateModel) => Promise<TerrariaInstanceEntity>
                >;
                let loadingWhileRunningInstance: boolean;
                const result: TerrariaInstanceEntity = {} as TerrariaInstanceEntity;
                let closeDialogSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => void>;

                beforeEach(() => {
                    updateInstanceSpy = spyOn(restApiService, 'updateInstance').and.callFake(() => {
                        loadingWhileRunningInstance = component.loading;
                        return Promise.resolve(result);
                    });
                    closeDialogSpy = spyOn(dialogRef, 'close');
                    dialog.clickButton('Run');
                });

                it('should run the instance and close the dialog', () => {
                    expect(updateInstanceSpy).toHaveBeenCalledOnceWith(2, {
                        runConfiguration: {
                            maxPlayers: 1,
                            port: 9999,
                            automaticallyForwardPort: false,
                            password: 'test-password',
                            worldId: worldWithMatchingModsId,
                        },
                    });
                });

                it('should be loading while running the instance', () => {
                    expect(loadingWhileRunningInstance).toBe(true);
                });

                it('should close the dialog', () => {
                    expect(closeDialogSpy).toHaveBeenCalledOnceWith(result);
                });

                it('should not be loading', () => {
                    expect(component.loading).toBeFalse();
                });
            });
        });

        describe('when invalid values have been entered', () => {
            beforeEach(fakeAsync(() => {
                component.maxPlayersInput.setValue(-1);
                component.portInput.setValue(0);
                refreshFixture(fixture);
            }));

            it('should have an invalid form', () => {
                expect(component.form.invalid).toBeTrue();
            });

            it('only the Cancel buttons should be enabled', () => {
                expect(dialog.enabledButtons).toEqual(['Cancel']);
            });

            describe('onRunClicked', () => {
                let updateInstanceSpy: jasmine.Spy;

                beforeEach(() => {
                    updateInstanceSpy = spyOn(restApiService, 'updateInstance');
                    component.onRunClicked();
                });

                it('should do nothing', () => {
                    expect(updateInstanceSpy).not.toHaveBeenCalled();
                });
            });
        });
    });

    describe('when there are no worlds', () => {
        beforeEach(async () => {
            hostWorlds = [];
            await init();
        });

        describe('the World form field', () => {
            let worldField: FormFieldInfo;

            beforeEach(() => {
                worldField = new MatFormFieldInfo(fixture, 3, component.worldInput);
            });

            it('should have no value', () => {
                expect(worldField.value).toBeUndefined();
            });

            it('should have an error', () => {
                expect(worldField.errors).toEqual(['There are no worlds to run this server with']);
            });
        });

        it('should have an invalid form', () => {
            expect(component.form.invalid).toBeTrue();
        });
    });
});
