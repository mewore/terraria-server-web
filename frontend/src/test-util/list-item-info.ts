import { ComponentFixture } from '@angular/core/testing';

export interface ListItemInfo {
    getLines(): string[];
    getLinkAtLine(lineIndex: number, linkLabel: string): string | undefined;
    isDisabled(): boolean | undefined;

    click(): Promise<void>;
}

export class MaterialListItemInfo implements ListItemInfo {
    private readonly rootElement: HTMLElement;

    constructor(private readonly fixture: ComponentFixture<any>) {
        this.rootElement = fixture.nativeElement;
    }

    getLines(): string[] {
        const lineElements = this.rootElement.getElementsByClassName('mat-line');
        const lines: string[] = [];
        for (let i = 0; i < lineElements.length; i++) {
            lines.push(lineElements.item(i)?.textContent?.trim().replace(/\s+/g, ' ') || '');
        }
        return lines;
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

    isDisabled(): boolean | undefined {
        return this.getItemElement()?.classList.contains('mat-list-item-disabled');
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

    private getItemElement(): HTMLElement | null {
        return this.rootElement.getElementsByClassName('mat-list-item').item(0) as HTMLElement | null;
    }
}
