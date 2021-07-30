import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TerrariaWorldListItemComponent } from './terraria-world-list-item.component';

xdescribe('TerrariaWorldListItemComponent', () => {
    let component: TerrariaWorldListItemComponent;
    let fixture: ComponentFixture<TerrariaWorldListItemComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TerrariaWorldListItemComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TerrariaWorldListItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
