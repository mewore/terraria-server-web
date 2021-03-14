import { TestBed } from '@angular/core/testing';

import { CreateTerrariaInstanceDialogService } from './create-terraria-instance-dialog.service';

describe('CreateTerrariaInstanceDialogService', () => {
    let service: CreateTerrariaInstanceDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CreateTerrariaInstanceDialogService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
