import { TestBed } from '@angular/core/testing';

import { AuthenticationDialogService } from './authentication-dialog.service';

describe('AuthenticationDialogService', () => {
    let service: AuthenticationDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(AuthenticationDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});