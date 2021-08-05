import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { TerrariaInstanceEntity } from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { MaterialSelectionListInfo, SelectionListInfo } from 'src/test-util/selection-list-info';
import { SetInstanceModsDialogComponent, SetInstanceModsDialogOutput } from './set-instance-mods-dialog.component';

describe('SetInstanceModsDialogComponent', () => {
    let fixture: ComponentFixture<SetInstanceModsDialogComponent>;
    let component: SetInstanceModsDialogComponent;

    let dialogRef: MatDialogRef<SetInstanceModsDialogComponent, SetInstanceModsDialogOutput>;
    let restApiService: RestApiService;
    let errorService: ErrorService;
    let instance: TerrariaInstanceEntity;
    let modList: SelectionListInfo;

    async function instantiate(): Promise<void> {
        fixture = TestBed.createComponent(SetInstanceModsDialogComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        component = fixture.componentInstance;
        modList = new MaterialSelectionListInfo(fixture);
    }

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
            declarations: [SetInstanceModsDialogComponent, TranslatePipeStub],
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
        await instantiate();
    });

    function getButton(label: string): HTMLButtonElement {
        const buttons = (fixture.nativeElement as Element).getElementsByTagName('button');
        for (let i = 0; i < buttons.length; i++) {
            const button = buttons.item(i);
            if (button && button.textContent?.trim() === label) {
                return button;
            }
        }
        throw new Error(`There is no '${label}' button`);
    }

    it('should create', () => {
        expect(component).toBeTruthy();
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

    describe('when the list property is not defined', () => {
        beforeEach(() => {
            component.list = undefined;
        });

        describe('when Set is clicked', () => {
            let setInstanceModsSpy: jasmine.Spy;
            let closeDialogSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => void>;
            let showErrorSpy: jasmine.Spy<(error: Error) => void>;

            beforeEach(fakeAsync(() => {
                setInstanceModsSpy = spyOn(restApiService, 'setInstanceMods');
                closeDialogSpy = spyOn(dialogRef, 'close');
                showErrorSpy = spyOn(errorService, 'showError').and.callFake(() => {});
                getButton('Set').click();
                tick();
            }));

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(new Error('The selected mods are unknown!'));
            });

            it('should not set the mods', () => {
                expect(setInstanceModsSpy).not.toHaveBeenCalled();
            });

            it('should not close the dialog', () => {
                expect(closeDialogSpy).not.toHaveBeenCalled();
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
            let setInstanceModsSpy: jasmine.Spy<
                (instanceId: number, mods: string[]) => Promise<TerrariaInstanceEntity>
            >;
            let loadingWhileSettingMods: boolean;
            const result: TerrariaInstanceEntity = {} as TerrariaInstanceEntity;
            let closeDialogSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => void>;

            beforeEach(fakeAsync(() => {
                setInstanceModsSpy = spyOn(restApiService, 'setInstanceMods').and.callFake(() => {
                    loadingWhileSettingMods = component.loading;
                    return Promise.resolve(result);
                });
                closeDialogSpy = spyOn(dialogRef, 'close');
                getButton('Set').click();
                tick();
            }));

            it('should set the mods', () => {
                expect(setInstanceModsSpy).toHaveBeenCalledOnceWith(2, ['second', 'third']);
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
