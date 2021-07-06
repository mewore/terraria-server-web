import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HostInfoPageComponent } from './host-info-page.component';

describe('HostInfoComponent', () => {
    let component: HostInfoPageComponent;
    let fixture: ComponentFixture<HostInfoPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HostInfoPageComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(HostInfoPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
