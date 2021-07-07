import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TerrariaInstanceListItemComponent } from './terraria-instance-list-item.component';

describe('TerrariaInstanceListItemComponent', () => {
    let component: TerrariaInstanceListItemComponent;
    let fixture: ComponentFixture<TerrariaInstanceListItemComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TerrariaInstanceListItemComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TerrariaInstanceListItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
