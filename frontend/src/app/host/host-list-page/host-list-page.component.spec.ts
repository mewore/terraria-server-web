import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HostListPageComponent } from './host-list-page.component';

xdescribe('HostListPageComponent', () => {
    let component: HostListPageComponent;
    let fixture: ComponentFixture<HostListPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HostListPageComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(HostListPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
