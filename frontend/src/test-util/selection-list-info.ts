import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';

export interface SelectionListInfo {
    readonly options: string[];
    readonly checkedOptions: string[];
    readonly uncheckedOptions: string[];

    clickOptions(...optionsToClick: string[]): void;
}

interface OptionInfo {
    label: string;
    checked: boolean;
    element: HTMLElement;
}

export class MaterialSelectionListInfo implements SelectionListInfo {
    private readonly rootElement: HTMLElement;

    constructor(private readonly fixture: ComponentFixture<any>) {
        this.rootElement = fixture.nativeElement;
    }

    get options(): string[] {
        return this.getOptionElements().map((option) => option.label);
    }

    get checkedOptions(): string[] {
        return this.getOptionElements()
            .filter((option) => option.checked)
            .map((option) => option.label);
    }

    get uncheckedOptions(): string[] {
        return this.getOptionElements()
            .filter((option) => !option.checked)
            .map((option) => option.label);
    }

    clickOptions(...optionsToClick: string[]): void {
        const optionMap = new Map<string, HTMLElement>();
        for (const option of this.getOptionElements()) {
            optionMap.set(option.label, option.element);
        }
        for (const optionToClick of optionsToClick) {
            const optionElement = optionMap.get(optionToClick);
            if (!optionElement) {
                throw new Error(
                    `Cannot click option '${optionToClick}' because it does not exist. The options are: ${this.options.join(
                        ', '
                    )}`
                );
            }
            optionElement.click();
        }
        fakeAsync(() => {
            this.fixture.detectChanges();
            tick();
        })();
    }

    private getOptionElements(): OptionInfo[] {
        const result: OptionInfo[] = [];
        const options = this.getListElement().getElementsByClassName('mat-list-option');
        for (let i = 0; i < options.length; i++) {
            const option = options.item(i);
            if (!option) {
                continue;
            }
            const checkbox = option.getElementsByClassName('mat-pseudo-checkbox').item(0);
            if (checkbox) {
                result.push({
                    label: option.textContent?.trim() || '',
                    checked: checkbox.classList.contains('mat-pseudo-checkbox-checked'),
                    element: option as HTMLElement,
                });
            }
        }
        return result;
    }

    private getListElement(): Element {
        const fieldElement = this.rootElement.getElementsByClassName('mat-selection-list').item(0);
        if (!fieldElement) {
            throw new Error(`There is no Material selection list`);
        }
        return fieldElement;
    }
}
