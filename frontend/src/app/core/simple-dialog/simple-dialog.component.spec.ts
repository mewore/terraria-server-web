import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogRefStub } from 'src/stubs/mat-dialog-ref.stub';
import { NoLanguageTranslatePipeStub, EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { DialogInfo, MaterialDialogInfo } from 'src/test-util/dialog-info';
import { SimpleDialogComponent, SimpleDialogInput } from './simple-dialog.component';

type DialogReturnType = string;

describe('SimpleDialogComponent', () => {
    let fixture: ComponentFixture<SimpleDialogComponent<DialogReturnType>>;
    let component: SimpleDialogComponent<DialogReturnType>;
    let dialog: DialogInfo;

    let dialogRef: MatDialogRef<SimpleDialogComponent<DialogReturnType>, DialogReturnType>;

    beforeEach(async () => {
        const data = {
            primaryButton: {
                labelKey: 'primary-button',
                onClicked: () => Promise.resolve('primary'),
            },
            titleKey: 'title',
            descriptionKey: 'description',
            extraButtons: [
                {
                    labelKey: 'extra-button',
                    onClicked: () => 'extra',
                },
            ],
        } as SimpleDialogInput<DialogReturnType>;

        await TestBed.configureTestingModule({
            imports: [MatDialogModule, MatButtonModule, MatProgressBarModule],
            declarations: [SimpleDialogComponent, NoLanguageTranslatePipeStub],
            providers: [
                { provide: MatDialogRef, useClass: MatDialogRefStub },
                { provide: MAT_DIALOG_DATA, useValue: data },
            ],
        }).compileComponents();

        dialogRef = TestBed.inject(MatDialogRef);

        [fixture, component] = await initComponent<SimpleDialogComponent<DialogReturnType>>(SimpleDialogComponent);
        dialog = new MaterialDialogInfo(fixture);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should not be loading', () => {
        expect(component.loading).toBeFalse();
    });

    it('should should have the correct title', () => {
        expect(dialog.title).toBe('translated(title)');
    });

    it('should should have the correct content', () => {
        expect(dialog.content).toBe('translated(description)');
    });

    it('should should have the correct buttons', () => {
        expect(dialog.buttons).toEqual([
            'translated(dialog.buttons.cancel)',
            'translated(extra-button)',
            'translated(primary-button)',
        ]);
    });

    it('all buttons should be enabled', () => {
        expect(dialog.enabledButtons).toEqual([
            'translated(dialog.buttons.cancel)',
            'translated(extra-button)',
            'translated(primary-button)',
        ]);
    });

    describe('while loading', () => {
        beforeEach(fakeAsync(() => {
            component.loading = true;
            fixture.detectChanges();
            tick();
        }));

        it('only the cancel button should be enabled', () => {
            expect(dialog.enabledButtons).toEqual(['translated(dialog.buttons.cancel)']);
        });
    });

    describe('when a button is clicked', () => {
        let closeSpy: jasmine.Spy<(value: DialogReturnType | undefined) => void>;
        let loadingWhileClosing: boolean;

        beforeEach(() => {
            closeSpy = spyOn(dialogRef, 'close').and.callFake(() => (loadingWhileClosing = component.loading));
        });

        describe('when the primary buttton is clicked', () => {
            beforeEach(() => dialog.clickButton('translated(primary-button)'));

            it('should close the dialog with its value', () => {
                expect(closeSpy).toHaveBeenCalledOnceWith('primary');
            });
        });

        describe('when the extra buttton is clicked', () => {
            beforeEach(() => dialog.clickButton('translated(extra-button)'));

            it('should close the dialog with its value', () => {
                expect(closeSpy).toHaveBeenCalledOnceWith('extra');
            });
        });

        describe('when the Cancel buttton is clicked', () => {
            beforeEach(() => dialog.clickButton('translated(dialog.buttons.cancel)'));

            it('should close the dialog with no value', () => {
                expect(closeSpy).toHaveBeenCalledOnceWith(undefined);
            });
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
