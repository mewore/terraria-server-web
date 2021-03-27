import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TerrariaWorldCardComponent } from './terraria-world-card.component';

describe('TerrariaWorldCardComponent', () => {
    let component: TerrariaWorldCardComponent;
    let fixture: ComponentFixture<TerrariaWorldCardComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TerrariaWorldCardComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TerrariaWorldCardComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
