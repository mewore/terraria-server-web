import { TestBed } from '@angular/core/testing';

import { RunServerDialogService } from './run-server-dialog.service';

describe('RunServerDialogService', () => {
    let service: RunServerDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(RunServerDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
