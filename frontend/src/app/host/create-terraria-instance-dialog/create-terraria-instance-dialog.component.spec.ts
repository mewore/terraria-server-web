import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import {
    TerrariaInstanceDefinitionModel,
    TerrariaInstanceEntity,
    TModLoaderVersionViewModel,
} from 'src/generated/backend';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';

import {
    CreateTerrariaInstanceDialogComponent,
    CreateTerrariaInstanceDialogInput,
    CreateTerrariaInstanceDialogOutput,
} from './create-terraria-instance-dialog.component';

describe('CreateTerrariaInstanceDialogComponent', () => {
    let component: CreateTerrariaInstanceDialogComponent;
    let fixture: ComponentFixture<CreateTerrariaInstanceDialogComponent>;

    let dialogRef: MatDialogRef<CreateTerrariaInstanceDialogComponent, CreateTerrariaInstanceDialogOutput>;
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
            imports: [MatDialogModule, ReactiveFormsModule, MatInputModule, MatSelectModule, MatProgressBarModule],
            declarations: [CreateTerrariaInstanceDialogComponent, TranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: MAT_DIALOG_DATA, useValue: data },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);

        restApiService = TestBed.inject(RestApiService);
        spyOn(restApiService, 'getTModLoaderVersions').and.callFake(() => {
            loadingWhileGettingVersions = component.loading;
            return Promise.resolve(tModLoaderVersions);
        });

        fixture = TestBed.createComponent(CreateTerrariaInstanceDialogComponent);
        await fixture.whenStable();
        component = fixture.componentInstance;
        await component.ngOnInit();
    });

    afterEach(() => {
        fixture.destroy();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should not be loading', () => {
        expect(component.loading).toBeFalse();
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

    describe('onCreateClicked', () => {
        let createInstanceSpy: jasmine.Spy<
            (hostId: number, model: TerrariaInstanceDefinitionModel) => Promise<TerrariaInstanceEntity>
        >;
        let closeDialogSpy: jasmine.Spy<(instance: TerrariaInstanceEntity) => void>;

        beforeEach(() => {
            createInstanceSpy = spyOn(restApiService, 'createTerrariaInstance');
            closeDialogSpy = spyOn(dialogRef, 'close');
        });

        describe('when the inputs are invalid', () => {
            beforeEach(async () => {
                await component.onCreateClicked();
            });

            it('should do nothing', () => {
                expect(createInstanceSpy).not.toHaveBeenCalled();
                expect(closeDialogSpy).not.toHaveBeenCalled();
            });

            it('should not be loading', () => {
                expect(component.loading).toBeFalse();
            });
        });

        describe('when the inputs are valid', () => {
            let loadingWhileCreating: boolean;
            let loadingWhileClosing: boolean;

            beforeEach(async () => {
                component.instanceNameInput.setValue('name');
                component.terrariaServerArchiveUrlInput.setValue('http://terraria.org/server-1353.zip');
                component.modLoaderReleaseInput.setValue(11);

                createInstanceSpy = createInstanceSpy.and.callFake(() => {
                    loadingWhileCreating = component.loading;
                    return Promise.resolve({ id: 18 } as TerrariaInstanceEntity);
                });
                closeDialogSpy = closeDialogSpy.and.callFake(() => (loadingWhileClosing = component.loading));
                await component.onCreateClicked();
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
