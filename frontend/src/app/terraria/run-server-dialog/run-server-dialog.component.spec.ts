import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RunServerDialogComponent } from './run-server-dialog.component';

xdescribe('RunServerDialogComponent', () => {
    let component: RunServerDialogComponent;
    let fixture: ComponentFixture<RunServerDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [RunServerDialogComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(RunServerDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
