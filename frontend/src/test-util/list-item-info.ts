import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { refreshFixture } from './angular-test-util';

export interface ListItemInfo {
    readonly lines: string[];
    readonly buttonLabels: string[] | undefined;
    readonly disabled: boolean | undefined;

    getLinkAtLine(lineIndex: number, linkLabel: string): string | undefined;
    getButton(label: string): HTMLButtonElement | undefined;

    click(): Promise<void>;
    clickButton(label: string): void;
}

export class MaterialListItemInfo implements ListItemInfo {
    private readonly rootElement: HTMLElement;

    constructor(private readonly fixture: ComponentFixture<any>) {
        this.rootElement = fixture.nativeElement;
    }

    get lines(): string[] {
        return Array.from(this.rootElement.querySelectorAll<HTMLElement>('.mat-line')).map((line) =>
            line.innerText.trim().replace(/\s+/g, ' ')
        );
    }

    get buttonLabels(): string[] | undefined {
        return this.getButtons()?.map((button) => button.innerText.trim());
    }

    get disabled(): boolean | undefined {
        return this.getItemElement()?.classList.contains('mat-list-item-disabled');
    }

    getLinkAtLine(lineIndex: number, linkLabel: string): string | undefined {
        const lineElement = this.rootElement.getElementsByClassName('mat-line').item(lineIndex);
        if (!lineElement) {
            throw new Error(`There is no line with index ${lineIndex}`);
        }
        const links = lineElement.getElementsByTagName('a');
        for (let i = 0; i < links.length; i++) {
            if (links.item(i)?.textContent?.trim() === linkLabel) {
                return links.item(i)?.getAttribute('href') ?? undefined;
            }
        }
        throw new Error(`There is no link with label '${linkLabel}' at line ${lineIndex}`);
    }

    getButton(label: string): HTMLButtonElement | undefined {
        return this.getButtons()?.find(button => button.innerText.trim() === label);
    }

    click(): Promise<void> {
        const itemElement = this.getItemElement();
        if (!itemElement) {
            throw new Error('The item element does not exist');
        }
        itemElement.click();
        this.fixture.detectChanges();
        return this.fixture.whenStable();
    }

    clickButton(label: string): void {
        const buttonToClick = this.getButtons()?.find(button => button.innerText?.trim() === label);
        if (!buttonToClick) {
            throw new Error(`There is no button with label "${label}"`);
        }
        if (buttonToClick.disabled) {
            throw new Error(`Cannot click the button with label "${label}" because it is disabled`);
        }
        fakeAsync(() => {
            buttonToClick.click();
            refreshFixture(this.fixture);
        })();
    }

    private getButtons(): HTMLButtonElement[] | undefined {
        const result = this.getItemElement()?.querySelectorAll('button');
        return result ? Array.from(result) : undefined;
    }

    private getItemElement(): HTMLElement | undefined {
        return this.rootElement.querySelector<HTMLElement>('.mat-list-item') || undefined;
    }
}
