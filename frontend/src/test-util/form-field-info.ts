import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { FormControl } from '@angular/forms';

export interface FormFieldInfo {
    readonly label: string | undefined;
    readonly errors: string[];
    readonly warnings: string[];

    readonly value: string | number;
    setValue(newValue: string | number | undefined): void;
}

export class MatFormFieldInfo implements FormFieldInfo {
    private readonly rootElement: HTMLElement;

    constructor(
        private readonly fixture: ComponentFixture<any>,
        private readonly index: number,
        private readonly control: FormControl
    ) {
        this.rootElement = fixture.nativeElement;
    }

    get label(): string | undefined {
        return this.getFieldElement().getElementsByTagName('mat-label').item(0)?.textContent?.trim();
    }

    get errors(): string[] {
        const errors: string[] = this.getContentOfElements(this.getFieldElement().getElementsByClassName('mat-error'));
        if (errors.length > 0 && this.control.valid) {
            throw new Error(`There are shown errors (${errors.join(', ')}) while the control is marked as valid`);
        }
        if (errors.length === 0 && this.control.invalid) {
            throw new Error(
                `There are no shown errors while the control is marked as invalid with errors: ${JSON.stringify(
                    this.control.errors
                )}`
            );
        }
        return errors;
    }

    get warnings(): string[] {
        const hintElement = this.getFieldElement().getElementsByClassName('mat-hint').item(0);
        if (!hintElement || !hintElement.classList.contains('warning')) {
            return [];
        }
        return this.getContentOfElements(hintElement.getElementsByTagName('span'));
    }

    get value(): string | number {
        return this.control.value;
    }

    setValue(newValue: string | number): void {
        fakeAsync(() => {
            this.control.setValue(newValue);
            this.fixture.detectChanges();
            tick();
        })();
    }

    private getContentOfElements(elements: HTMLCollectionOf<Element>): string[] {
        const content: string[] = [];
        for (let i = 0; i < elements.length; i++) {
            content.push(elements.item(i)?.textContent?.trim() || '');
        }
        return content;
    }

    private getFieldElement(): Element {
        const fieldElement = this.rootElement.getElementsByClassName('mat-form-field').item(this.index);
        if (!fieldElement) {
            throw new Error(`There is no Material field with index ${this.index}`);
        }
        return fieldElement;
    }
}
