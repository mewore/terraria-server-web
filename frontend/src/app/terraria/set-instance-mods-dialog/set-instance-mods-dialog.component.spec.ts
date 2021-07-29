import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetInstanceModsDialogComponent } from './set-instance-mods-dialog.component';

describe('SetInstanceModsDialogComponent', () => {
    let component: SetInstanceModsDialogComponent;
    let fixture: ComponentFixture<SetInstanceModsDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SetInstanceModsDialogComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(SetInstanceModsDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
