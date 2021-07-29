import { TestBed } from '@angular/core/testing';

import { SetInstanceModsDialogService } from './set-instance-mods-dialog.service';

describe('SetInstanceModsDialogService', () => {
    let service: SetInstanceModsDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(SetInstanceModsDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
