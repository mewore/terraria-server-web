import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { TerrariaInstanceEntity, TerrariaInstanceUpdateModel } from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo as MaterialDialogInfo } from 'src/test-util/dialog-info';
import { MaterialSelectionListInfo, SelectionListInfo } from 'src/test-util/selection-list-info';
import { SetInstanceModsDialogComponent, SetInstanceModsDialogOutput } from './set-instance-mods-dialog.component';

describe('SetInstanceModsDialogComponent', () => {
    let fixture: ComponentFixture<SetInstanceModsDialogComponent>;
    let component: SetInstanceModsDialogComponent;
    let modList: SelectionListInfo;
    let dialog: DialogInfo;

    let dialogRef: MatDialogRef<SetInstanceModsDialogComponent, SetInstanceModsDialogOutput>;
    let restApiService: RestApiService;
    let errorService: ErrorService;
    let instance: TerrariaInstanceEntity;

    beforeEach(async () => {
        instance = {
            id: 2,
        } as TerrariaInstanceEntity;

        instance.options = {
            1: 'first (enabled)',
            2: 'second (enabled)',
            3: 'third (disabled)',
            4: 'fourth (disabled)',
            1259: 'unrelated',
        };

        await TestBed.configureTestingModule({
            imports: [MatDialogModule, MatListModule, MatProgressBarModule, NoopAnimationsModule],
            declarations: [SetInstanceModsDialogComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
                { provide: MAT_DIALOG_DATA, useValue: instance },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);

        restApiService = TestBed.inject(RestApiService);
        errorService = TestBed.inject(ErrorService);

        [fixture, component] = await initComponent(SetInstanceModsDialogComponent);
        modList = new MaterialSelectionListInfo(fixture);
        dialog = new MaterialDialogInfo(fixture);
    });

    it('should have the correct title', () => {
        expect(dialog.title).toBe('Set the enabled mods');
    });

    it('should have the correct buttons', () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Set']);
    });

    it('should have the correct options', () => {
        expect(modList.options).toEqual(['first', 'second', 'third', 'fourth']);
    });

    it('should have only the enabled mods as checked options', () => {
        expect(modList.checkedOptions).toEqual(['first', 'second']);
    });

    it('should have only the disabled mods as unchecked options', () => {
        expect(modList.uncheckedOptions).toEqual(['third', 'fourth']);
    });

    it('should should have the correct buttons', () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Set']);
    });

    it('all buttons should be enabled', () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Set']);
    });

    describe('while loading', () => {
        beforeEach(fakeAsync(() => {
            component.loading = true;
            refreshFixture(fixture);
        }));

        it('only the cancel button should be enabled', () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });
    });

    describe('when the list property is not defined', () => {
        beforeEach(() => {
            component.list = undefined;
        });

        describe('when Set is clicked', () => {
            let showErrorSpy: jasmine.Spy<(error: Error) => void>;

            beforeEach(() => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                dialog.clickButton('Set');
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(new Error('The selected mods are unknown!'));
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });
    });

    describe('when some of the options are clicked', () => {
        beforeEach(() => {
            modList.clickOptions('first', 'third');
        });

        it('should have other checked options', () => {
            expect(modList.checkedOptions).toEqual(['second', 'third']);
        });

        describe('when Set is clicked', () => {
            let updateInstanceSpy: jasmine.Spy<
                (instanceId: number, model: TerrariaInstanceUpdateModel) => Promise<TerrariaInstanceEntity>
            >;
            let loadingWhileSettingMods: boolean;
            const result: TerrariaInstanceEntity = {} as TerrariaInstanceEntity;
            let closeDialogSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => void>;

            beforeEach(() => {
                updateInstanceSpy = spyOn(restApiService, 'updateInstance').and.callFake(() => {
                    loadingWhileSettingMods = component.loading;
                    return Promise.resolve(result);
                });
                closeDialogSpy = spyOn(dialogRef, 'close');
                dialog.clickButton('Set');
            });

            it('should set the mods', () => {
                expect(updateInstanceSpy).toHaveBeenCalledOnceWith(2, { newMods: ['second', 'third'] });
            });

            it('should be loading while setting the mods', () => {
                expect(loadingWhileSettingMods).toBe(true);
            });

            it('should close the dialog', () => {
                expect(closeDialogSpy).toHaveBeenCalledOnceWith(result);
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });
    });
});
