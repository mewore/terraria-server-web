import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { ErrorService } from 'src/app/core/services/error.service';
import { ErrorServiceStub } from 'src/app/core/services/error.service.stub';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { SimpleDialogInput } from 'src/app/core/simple-dialog/simple-dialog.component';
import { SimpleDialogService } from 'src/app/core/simple-dialog/simple-dialog.service';
import { SimpleDialogServiceStub } from 'src/app/core/simple-dialog/simple-dialog.service.stub';
import { TerrariaWorldEntity } from 'src/generated/backend';
import { EnUsTranslateServiceStub } from 'src/stubs/translate.service.stub';

import { TerrariaWorldService, TerrariaWorldServiceImpl } from './terraria-world.service';

describe('TerrariaWorldService', () => {
    let service: TerrariaWorldService;

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
                { provide: TranslateService, useClass: EnUsTranslateServiceStub },
            ],
        });
        translateService = TestBed.inject(TranslateService);

        restApiService = TestBed.inject(RestApiService);
        simpleDialogService = TestBed.inject(SimpleDialogService);
        errorService = TestBed.inject(ErrorService);

        service = TestBed.inject(TerrariaWorldServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('canDelete', () => {
        it('should return true for unused worlds', () => {
            expect(service.canDelete({ id: 1 } as TerrariaWorldEntity, new Set<number>())).toBeTrue();
        });

        it('should return false for used worlds', () => {
            expect(service.canDelete({ id: 1 } as TerrariaWorldEntity, new Set<number>([1]))).toBeFalse();
        });

        it('should return false for undefined worlds', () => {
            expect(service.canDelete(undefined, new Set<number>())).toBeFalse();
        });
    });

    describe('deleting a world', () => {
        let result: boolean | undefined;

        describe('when the confirmation dialog is confirmed', () => {
            let simpleDialogData: SimpleDialogInput<TerrariaWorldEntity>;
            let deleteSpy: jasmine.Spy<(worldId: number) => Promise<void>>;
            const newWorld = {} as TerrariaWorldEntity;

            beforeEach(async () => {
                spyOn(simpleDialogService, 'openDialog').and.callFake(async <T>(data: SimpleDialogInput<T>) => {
                    simpleDialogData = data as SimpleDialogInput<any>;
                    return await data.primaryButton.onClicked();
                });
                deleteSpy = spyOn(restApiService, 'deleteWorld').and.resolveTo();
                result = await service.delete({ id: 1 } as TerrariaWorldEntity);
            });

            it('should open the confirmation dialog with the correct data', () => {
                expect([
                    translateService.instant(simpleDialogData.titleKey),
                    translateService.instant(simpleDialogData.descriptionKey || 'NO_DESCRIPTION_KEY'),
                    `[${translateService.instant(simpleDialogData.primaryButton.labelKey)}]`,
                ]).toEqual([
                    'Delete the world?',
                    'This is irreversible. The data of the world will be deleted forever. ' +
                        'You might want to download it first just in case you need it later.',
                    '[Delete]',
                ]);
                expect(simpleDialogData.extraButtons).toBeUndefined();
            });

            it('should open the confirmation dialog as a warning', () => {
                expect(simpleDialogData.warn).toBeTrue();
            });

            it('should request to delete the world', () => {
                expect(deleteSpy).toHaveBeenCalledOnceWith(1);
            });

            it('should return true', () => {
                expect(result).toBe(true);
            });
        });

        describe('when the confirmation dialog is cancelled', () => {
            beforeEach(async () => {
                spyOn(simpleDialogService, 'openDialog').and.resolveTo(undefined);
                result = await service.delete({ id: 1 } as TerrariaWorldEntity);
            });

            it('should return undefined', () => {
                expect(result).toBeUndefined();
            });
        });

        describe('when called with an undefined world', () => {
            let showErrorSpy: jasmine.Spy<(error: Error | string) => void>;

            beforeEach(async () => {
                showErrorSpy = spyOn(errorService, 'showError').and.returnValue();
                result = await service.delete(undefined);
            });

            it('should show an error', () => {
                expect(showErrorSpy).toHaveBeenCalledOnceWith('Cannot delete the world because it is undefined');
            });

            it('should return undefined', () => {
                expect(result).toBeFalse();
            });
        });
    });
});
