import { TestBed } from '@angular/core/testing';

import { SimpleDialogService } from './simple-dialog.service';

describe('SimpleDialogService', () => {
    let service: SimpleDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(SimpleDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
