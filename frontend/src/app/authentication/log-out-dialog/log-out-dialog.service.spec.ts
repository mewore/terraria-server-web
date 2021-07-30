import { TestBed } from '@angular/core/testing';

import { LogOutDialogService } from './log-out-dialog.service';

xdescribe('LogOutDialogService', () => {
    let service: LogOutDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(LogOutDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
