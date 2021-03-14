import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateTerrariaInstanceDialogComponent } from './create-terraria-instance-dialog.component';

describe('CreateTerrariaInstanceDialogComponent', () => {
    let component: CreateTerrariaInstanceDialogComponent;
    let fixture: ComponentFixture<CreateTerrariaInstanceDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [CreateTerrariaInstanceDialogComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(CreateTerrariaInstanceDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
