import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HostListItemComponent } from './host-list-item.component';

describe('HostListItemComponent', () => {
    let component: HostListItemComponent;
    let fixture: ComponentFixture<HostListItemComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HostListItemComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(HostListItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
