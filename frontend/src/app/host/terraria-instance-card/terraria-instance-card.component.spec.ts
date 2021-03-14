import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TerrariaInstanceCardComponent } from './terraria-instance-card.component';

describe('TerrariaInstanceCardComponent', () => {
    let component: TerrariaInstanceCardComponent;
    let fixture: ComponentFixture<TerrariaInstanceCardComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TerrariaInstanceCardComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TerrariaInstanceCardComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
