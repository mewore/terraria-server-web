import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TerrariaInstancePageComponent } from './terraria-instance-page.component';

xdescribe('TerrariaInstancePageComponent', () => {
    let component: TerrariaInstancePageComponent;
    let fixture: ComponentFixture<TerrariaInstancePageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TerrariaInstancePageComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TerrariaInstancePageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
