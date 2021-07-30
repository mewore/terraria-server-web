import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleDialogComponent } from './simple-dialog.component';

xdescribe('SimpleDialogComponent', () => {
    let component: SimpleDialogComponent<unknown>;
    let fixture: ComponentFixture<SimpleDialogComponent<unknown>>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SimpleDialogComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(SimpleDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
