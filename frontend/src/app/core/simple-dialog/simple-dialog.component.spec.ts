import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { SimpleDialogComponent, SimpleDialogInput } from './simple-dialog.component';

type DialogReturnType = string;

describe('SimpleDialogComponent', () => {
    let component: SimpleDialogComponent<DialogReturnType>;
    let fixture: ComponentFixture<SimpleDialogComponent<DialogReturnType>>;

    let dialogRef: MatDialogRef<SimpleDialogComponent<DialogReturnType>, DialogReturnType>;
    let data: SimpleDialogInput<DialogReturnType>;

    beforeEach(async () => {
        data = {
            primaryButton: {
                labelKey: 'primary-button',
                onClicked: () => Promise.resolve('primary'),
            },
            titleKey: 'title',
            descriptionKey: 'description',
            extraButtons: [
                {
                    labelKey: 'extra-button',
                    onClicked: () => Promise.resolve('extra'),
                },
            ],
        };
        await TestBed.configureTestingModule({
            imports: [MatDialogModule, MatButtonModule, MatProgressBarModule],
            declarations: [SimpleDialogComponent, TranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: MAT_DIALOG_DATA, useValue: data },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);

        fixture = TestBed.createComponent(SimpleDialogComponent) as ComponentFixture<
            SimpleDialogComponent<DialogReturnType>
        >;
        await fixture.whenStable();
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should not be loading', () => {
        expect(component.loading).toBeFalse();
    });

    describe('when a button is clicked', () => {
        let closeSpy: jasmine.Spy<(value: DialogReturnType) => void>;
        let loadingWhileClosing: boolean;

        beforeEach(() => {
            closeSpy = spyOn(dialogRef, 'close').and.callFake(() => (loadingWhileClosing = component.loading));
        });

        describe('when the result is a value', () => {
            let loadingWhileExecutingButtonFn: boolean;

            beforeEach(async () => {
                await component.onButtonClicked({
                    labelKey: 'label',
                    onClicked: () => {
                        loadingWhileExecutingButtonFn = component.loading;
                        return 'result';
                    },
                });
            });

            it('should be loading while executing the button function', () => {
                expect(loadingWhileExecutingButtonFn).toBeTrue();
            });

            it('should be loading while closing', () => {
                expect(loadingWhileClosing).toBeTrue();
            });

            it('should not be loading at the end', () => {
                expect(component.loading).toBeFalse();
            });

            it('should close with the result', () => {
                expect(closeSpy).toHaveBeenCalledOnceWith('result');
            });
        });

        describe('when the result is a promise', () => {
            beforeEach(async () => {
                await component.onButtonClicked({
                    labelKey: 'label',
                    onClicked: () => Promise.resolve('result'),
                });
            });

            it('should close with the result', () => {
                expect(closeSpy).toHaveBeenCalledOnceWith('result');
            });
        });
    });
});
