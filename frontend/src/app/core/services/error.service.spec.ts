import { TestBed } from '@angular/core/testing';

import { ErrorService, ErrorServiceImpl } from './error.service';

describe('ErrorService', () => {
    let service: ErrorService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(ErrorServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('showError', () => {
        let error: Error;
        let consoleErrorSpy: jasmine.Spy;

        beforeEach(() => {
            error = {} as Error;
            consoleErrorSpy = spyOn(console, 'error').and.returnValue();
            service.showError(error);
        });

        it('should call console.error with the same error', () => {
            expect(consoleErrorSpy).toHaveBeenCalledOnceWith(error);
        });
    });
});
