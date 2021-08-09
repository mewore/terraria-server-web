import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { SimpleDialogInput } from 'src/app/core/simple-dialog/simple-dialog.component';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { SimpleDialogServiceStub } from 'src/app/core/simple-dialog/simple-dialog.service.stub';
import { TerrariaInstanceEntity, TerrariaInstanceUpdateModel } from 'src/generated/backend';
import { EnUsTranslateServiceStub } from 'src/stubs/translate.service.stub';
import { TerrariaInstanceService, TerrariaInstanceServiceImpl } from './terraria-instance.service';

describe('TerrariaInstanceService', () => {
    let service: TerrariaInstanceService;

    let restApiService: RestApiService;
    let simpleDialogService: SimpleDialogService;
    let errorService: ErrorService;

    let translateService: TranslateService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: SimpleDialogService, useClass: SimpleDialogServiceStub },
                { provide: RestApiService, useClass: RestApiServiceStub },
                { provide: ErrorService, useClass: ErrorServiceStub },
            ],
        });
        translateService = new EnUsTranslateServiceStub().masked();

        restApiService = TestBed.inject(RestApiService);
        simpleDialogService = TestBed.inject(SimpleDialogService);
        errorService = TestBed.inject(ErrorService);

        service = TestBed.inject(TerrariaInstanceServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('isRunning', () => {
        it('should return true for running instances', () => {
            expect(service.isRunning({ state: 'RUNNING' } as TerrariaInstanceEntity)).toBeTrue();
        });

        it('should return false for idle instances', () => {
            expect(service.isRunning({ state: 'IDLE' } as TerrariaInstanceEntity)).toBeFalse();
        });
    });

    describe('canDelete', () => {
        it('should return true for idle instances', () => {
            expect(service.canDelete({ state: 'IDLE' } as TerrariaInstanceEntity)).toBeTrue();
        });

        it('should return false for running instances', () => {
            expect(service.canDelete({ state: 'RUNNING' } as TerrariaInstanceEntity)).toBeFalse();
        });

        it('should return false for undefined instances', () => {
            expect(service.canDelete(undefined)).toBeFalse();
        });
    });

    type UpdateInstanceFn = (instanceId: number, model: TerrariaInstanceUpdateModel) => Promise<TerrariaInstanceEntity>;

    describe('deleting an instance', () => {
        let result: TerrariaInstanceEntity | undefined;

        describe('when the confirmation dialog is confirmed', () => {
            let simpleDialogData: SimpleDialogInput<TerrariaInstanceEntity>;
            let requestActionSpy: jasmine.Spy<UpdateInstanceFn>;
            const newInstance = {} as TerrariaInstanceEntity;

            beforeEach(async () => {
                spyOn(simpleDialogService, 'openDialog').and.callFake(async <T>(data: SimpleDialogInput<T>) => {
                    simpleDialogData = data as SimpleDialogInput<any>;
                    return await data.primaryButton.onClicked();
                });
                requestActionSpy = spyOn(restApiService, 'updateInstance').and.resolveTo(newInstance);
                result = await service.delete({ id: 1, state: 'IDLE' } as TerrariaInstanceEntity);
            });

            it('should open the confirmation dialog with the correct data', () => {
                expect([
                    translateService.instant(simpleDialogData.titleKey),
                    translateService.instant(simpleDialogData.descriptionKey || 'NO_DESCRIPTION_KEY'),
                    `[${translateService.instant(simpleDialogData.primaryButton.labelKey)}]`,
                ]).toEqual([
                    'Delete the instance?',
                    'This is irreversible. ' +
                        'The executable files and output history of this instance will be deleted forever.',
                    '[Delete]',
                ]);
                expect(simpleDialogData.extraButtons).toBeUndefined();
            });

            it('should open the confirmation dialog as a warning', () => {
                expect(simpleDialogData.warn).toBeTrue();
            });

            it('should request to delete the instance', () => {
                expect(requestActionSpy).toHaveBeenCalledOnceWith(1, { newAction: 'DELETE' });
            });

            it('should return the instance', () => {
                expect(result).toBe(newInstance);
            });
        });

        describe('when the confirmation dialog is cancelled', () => {
            beforeEach(async () => {
                spyOn(simpleDialogService, 'openDialog').and.resolveTo(undefined);
                result = await service.delete({ id: 1, state: 'IDLE' } as TerrariaInstanceEntity);
            });

            it('should return undefined', () => {
                expect(result).toBeUndefined();
            });
        });

        describe('when called with an instance in a non-deletable state', () => {
            let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

            beforeEach(async () => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                result = await service.delete({ state: 'RUNNING' } as TerrariaInstanceEntity);
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(
                    'Cannot delete the instance because it is in an invalid state: RUNNING'
                );
            });

            it('should return undefined', () => {
                expect(result).toBeUndefined();
            });
        });

        describe('when called with an undefined instance', () => {
            let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

            beforeEach(async () => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                result = await service.delete(undefined);
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith(
                    'Cannot delete the instance because it is in an invalid state: undefined'
                );
            });

            it('should return undefined', () => {
                expect(result).toBeUndefined();
            });
        });
    });
});
