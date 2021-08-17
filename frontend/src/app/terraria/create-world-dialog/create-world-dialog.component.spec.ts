import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { TerrariaInstanceEntity, TerrariaWorldEntity, TerrariaInstanceUpdateModel } from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo } from 'src/test-util/dialog-info';
import { FormFieldInfo, MatFormFieldInfo } from 'src/test-util/form-field-info';
import {
    CreateWorldDialogComponent,
    CreateWorldDialogInput,
    CreateWorldDialogOutput,
} from './create-world-dialog.component';

describe('CreateWorldDialogComponent', () => {
    let fixture: ComponentFixture<CreateWorldDialogComponent>;
    let component: CreateWorldDialogComponent;
    let dialog: DialogInfo;

    let dialogRef: MatDialogRef<CreateWorldDialogComponent, CreateWorldDialogOutput>;
    let restApiService: RestApiService;
    let instance: TerrariaInstanceEntity;
    let data: CreateWorldDialogInput;

    let getHostWorldsSpy: jasmine.Spy<(hostId: number) => Promise<TerrariaWorldEntity[]>>;

    const init = async (): Promise<void> => {
        [fixture, component] = await initComponent(CreateWorldDialogComponent);
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

        await TestBed.configureTestingModule({
            imports: [
                MatDialogModule,
                ReactiveFormsModule,
                MatInputModule,
                MatSelectModule,
                MatProgressBarModule,
                MatIconModule,
                MatTooltipModule,
                NoopAnimationsModule,
            ],
            declarations: [CreateWorldDialogComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: MAT_DIALOG_DATA, useValue: data },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);

        restApiService = TestBed.inject(RestApiService);
        getHostWorldsSpy = spyOn(restApiService, 'getHostWorlds').and.resolveTo([
            { displayName: 'Some _ World' } as TerrariaWorldEntity,
        ]);
        await init();
    });

    it('should have the correct title', () => {
        expect(dialog.title).toBe('Create a world with this Terraria instance');
    });

    it('should have the correct buttons', () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Create']);
    });

    it('should not be loading', () => {
        expect(component.loading).toBeFalse();
    });

    it('should fetch the host worlds', () => {
        expect(getHostWorldsSpy).toHaveBeenCalledOnceWith(1);
    });

    describe('fileName', () => {
        describe('when the name contains spaces', () => {
            beforeEach(() => {
                component.nameInput.setValue(' New World  ');
            });

            it('should be the trimmed name with underscores instead of spaces', () => {
                expect(component.fileName).toBe('New_World');
            });
        });
    });

    describe('the Name form field', () => {
        let nameField: FormFieldInfo;

        beforeEach(() => {
            nameField = new MatFormFieldInfo(fixture, 0, component.nameInput);
        });

        it('should have the correct label', () => {
            expect(nameField.label).toBe('World name');
        });

        it('should have the correct default value', () => {
            expect(nameField.value).toBe('');
        });

        describe('when a valid value is entered', () => {
            beforeEach(() => {
                nameField.setValue('New World');
            });

            it('should not have any errors', () => {
                expect(nameField.errors).toEqual([]);
            });
        });

        describe('when a blank value is entered', () => {
            beforeEach(() => {
                nameField.setValue('   ');
            });

            it('should have an error', () => {
                expect(nameField.errors).toEqual(['* Required (and whitespace-only values are not allowed)']);
            });
        });

        describe('when a string with length 256 is entered', () => {
            beforeEach(() => {
                const characters = [];
                for (let i = 0; i < 256; i++) {
                    characters.push('a');
                }
                nameField.setValue(characters.join(''));
            });

            it('should have an error', () => {
                expect(nameField.errors).toEqual(['Too long (max length: 255)']);
            });
        });

        describe('when an existing display name is entered', () => {
            beforeEach(() => {
                nameField.setValue('Some_  World');
            });

            it('should have an error', () => {
                expect(nameField.errors).toEqual(['There is already a world with the same file name: Some___World']);
            });
        });
    });

    describe('the Size form field', () => {
        let sizeField: FormFieldInfo;

        beforeEach(() => {
            sizeField = new MatFormFieldInfo(fixture, 1, component.sizeInput);
        });

        it('should have the correct label', () => {
            expect(sizeField.label).toBe('Size');
        });

        it('should have the correct default value', () => {
            expect(sizeField.value).toBe('SMALL');
        });

        it('should not have any errors', () => {
            expect(sizeField.errors).toEqual([]);
        });

        describe('when no size is selected', () => {
            beforeEach(() => {
                sizeField.setValue(undefined);
            });

            it('should have an error', () => {
                expect(sizeField.errors).toEqual(['* Required']);
            });
        });
    });

    describe('the Difficulty form field', () => {
        let difficultyField: FormFieldInfo;

        beforeEach(() => {
            difficultyField = new MatFormFieldInfo(fixture, 2, component.difficultyInput);
        });

        it('should have the correct label', () => {
            expect(difficultyField.label).toBe('Difficulty');
        });

        it('should have the correct default value', () => {
            expect(difficultyField.value).toBe('NORMAL');
        });

        it('should not have any errors', () => {
            expect(difficultyField.errors).toEqual([]);
        });

        describe('when no size is selected', () => {
            beforeEach(() => {
                difficultyField.setValue(undefined);
            });

            it('should have an error', () => {
                expect(difficultyField.errors).toEqual(['* Required']);
            });
        });
    });

    describe('when valid values have been entered', () => {
        beforeEach(fakeAsync(() => {
            component.nameInput.setValue('New World');
            component.sizeInput.setValue('SMALL');
            component.difficultyInput.setValue('NORMAL');
            refreshFixture(fixture);
        }));

        it('should have a valid form', () => {
            expect(component.form.valid).toBeTrue();
        });

        it('all buttons should be enabled', () => {
            expect(dialog.enabledButtons).toEqual(['Cancel', 'Create']);
        });

        describe('clicking on Create', () => {
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
                dialog.clickButton('Create');
            });

            it('should run the instance and close the dialog', () => {
                expect(updateInstanceSpy).toHaveBeenCalledOnceWith(2, {
                    worldCreationConfiguration: {
                        worldSize: 'SMALL',
                        worldDifficulty: 'NORMAL',
                        worldDisplayName: 'New World',
                    },
                });
            });

            it('should be loading while creating the world', () => {
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
            component.nameInput.setValue('  ');
            refreshFixture(fixture);
        }));

        it('should have an invalid form', () => {
            expect(component.form.invalid).toBeTrue();
        });

        it('only the Cancel buttons should be enabled', () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        describe('onCreateClicked', () => {
            let updateInstanceSpy: jasmine.Spy;

            beforeEach(() => {
                updateInstanceSpy = spyOn(restApiService, 'updateInstance');
                component.onCreateClicked();
            });

            it('should do nothing', () => {
                expect(updateInstanceSpy).not.toHaveBeenCalled();
            });
        });
    });
});
