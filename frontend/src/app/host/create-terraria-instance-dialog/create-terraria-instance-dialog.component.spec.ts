import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import {
    TerrariaInstanceDefinitionModel,
    TerrariaInstanceEntity,
    TModLoaderVersionViewModel,
} from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo } from 'src/test-util/dialog-info';

import {
    CreateTerrariaInstanceDialogComponent,
    CreateTerrariaInstanceDialogInput,
} from './create-terraria-instance-dialog.component';

describe('CreateTerrariaInstanceDialogComponent', () => {
    let fixture: ComponentFixture<CreateTerrariaInstanceDialogComponent>;
    let component: CreateTerrariaInstanceDialogComponent;
    let dialog: DialogInfo;

    let closeDialogSpy: jasmine.Spy<(instance: TerrariaInstanceEntity | undefined) => void>;
    let restApiService: RestApiService;
    let data: CreateTerrariaInstanceDialogInput;

    let tModLoaderVersions: TModLoaderVersionViewModel[];
    let loadingWhileGettingVersions: boolean;

    beforeEach(async () => {
        data = {
            id: 1,
            uuid: 'uuid',
            alive: true,
            os: 'LINUX',
            terrariaInstanceDirectory: 'instance-dir',
        };
        tModLoaderVersions = [
            {
                releaseId: 10,
                version: '1.10',
            },
            {
                releaseId: 11,
                version: '1.11',
            },
        ];

        await TestBed.configureTestingModule({
            imports: [
                MatDialogModule,
                ReactiveFormsModule,
                MatInputModule,
                MatSelectModule,
                MatProgressBarModule,
                NoopAnimationsModule,
            ],
            declarations: [CreateTerrariaInstanceDialogComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: MAT_DIALOG_DATA, useValue: data },
            ],
        }).compileComponents();

        const dialogRef = TestBed.inject(MatDialogRef);
        closeDialogSpy = spyOn(dialogRef, 'close');

        restApiService = TestBed.inject(RestApiService);

        [fixture, component] = await initComponent(CreateTerrariaInstanceDialogComponent, (createdComponent) => {
            spyOn(restApiService, 'getTModLoaderVersions').and.callFake(() => {
                loadingWhileGettingVersions = createdComponent.loading;
                return Promise.resolve(tModLoaderVersions);
            });
        });
        dialog = new MaterialDialogInfo(fixture);
    });

    it('should have the correct title', () => {
        expect(dialog.title).toBe('New Terraria server instance');
    });

    it('should have the correct buttons', () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Create']);
    });

    it('all buttons should be enabled', () => {
        expect(dialog.buttons).toEqual(['Cancel', 'Create']);
    });

    it('should not be loading', () => {
        expect(component.loading).toBeFalse();
    });

    it('should not have a loading indicator', () => {
        expect(dialog.hasLoadingIndicator).toBeFalse();
    });

    it('should have the tModLoader versions', () => {
        expect(component.tModLoaderVersions).toBe(tModLoaderVersions);
    });

    it('should have selected the first tModLoader release automatically', () => {
        expect(component.modLoaderReleaseInput.value).toBe(10);
    });

    it('should have enabled the tModLoader release input', () => {
        expect(component.modLoaderReleaseInput.enabled).toBeTrue();
    });

    describe('while fetching the tModLoader versions', () => {
        it('should be loading', () => {
            expect(loadingWhileGettingVersions).toBeTrue();
        });
    });

    describe('while loading', () => {
        beforeEach(fakeAsync(() => {
            component.loading = true;
            fixture.detectChanges();
            tick();
        }));

        it('only the Cancel button should be enabled', () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        it('should have a loading indicator', () => {
            expect(dialog.hasLoadingIndicator).toBeTrue();
        });
    });

    describe('when Cancel is clicked', () => {
        beforeEach(() => {
            dialog.clickButton('Cancel');
        });

        it('should close the dialog with no value', () => {
            expect(closeDialogSpy).toHaveBeenCalledOnceWith(undefined);
        });
    });

    describe('when the inputs are invalid', () => {
        beforeEach(async () => {
            await component.onCreateClicked();
        });

        it('only the Cancel button should be enabled', () => {
            expect(dialog.enabledButtons).toEqual(['Cancel']);
        });

        it('should not close the dialog', () => {
            expect(closeDialogSpy).not.toHaveBeenCalled();
        });

        it('should not be loading', () => {
            expect(component.loading).toBeFalse();
        });
    });

    describe('when the inputs are valid', () => {
        beforeEach(() => {
            component.instanceNameInput.setValue('name');
            component.terrariaServerArchiveUrlInput.setValue('http://terraria.org/server-1353.zip');
            component.modLoaderReleaseInput.setValue(11);
            fixture.detectChanges();
        });

        describe('when Create is clicked', () => {
            let createInstanceSpy: jasmine.Spy<
                (hostId: number, model: TerrariaInstanceDefinitionModel) => Promise<TerrariaInstanceEntity>
            >;
            let loadingWhileCreating: boolean;
            let loadingWhileClosing: boolean;

            beforeEach(() => {
                createInstanceSpy = spyOn(restApiService, 'createTerrariaInstance').and.callFake(() => {
                    loadingWhileCreating = component.loading;
                    return Promise.resolve({ id: 18 } as TerrariaInstanceEntity);
                });
                closeDialogSpy = closeDialogSpy.and.callFake(() => (loadingWhileClosing = component.loading));
                dialog.clickButton('Create');
            });

            it('should create a new instance', () => {
                expect(createInstanceSpy).toHaveBeenCalledOnceWith(1, {
                    instanceName: 'name',
                    terrariaServerArchiveUrl: 'http://terraria.org/server-1353.zip',
                    modLoaderReleaseId: 11,
                });
            });

            it('should be loading while creating the instance', () => {
                expect(loadingWhileCreating).toBeTrue();
            });

            it('should close the dialog with the created instance', () => {
                expect(closeDialogSpy).toHaveBeenCalledOnceWith({ id: 18 } as TerrariaInstanceEntity);
            });

            it('should be loading while closing the dialog', () => {
                expect(loadingWhileClosing).toBeTrue();
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });
    });
});
