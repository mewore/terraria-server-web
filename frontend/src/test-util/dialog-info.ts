import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';

export interface DialogInfo {
    readonly title: string | undefined;
    readonly content: string | undefined;
    readonly buttons: string[];
    readonly enabledButtons: string[];
    readonly hasLoadingIndicator: boolean | undefined;

    clickButton(label: string): void;
}

export class MaterialDialogInfo implements DialogInfo {
    private readonly rootElement: HTMLElement;

    constructor(private readonly fixture: ComponentFixture<any>) {
        this.rootElement = fixture.nativeElement;
    }

    get title(): string | undefined {
        return this.rootElement.querySelector<HTMLElement>('.mat-dialog-title')?.innerText;
    }

    get content(): string | undefined {
        return this.rootElement.querySelector<HTMLElement>('.mat-dialog-content')?.innerText;
    }

    get buttons(): string[] {
        return this.buttonElements.map((button) => button.innerText);
    }

    get enabledButtons(): string[] {
        return this.buttonElements.filter((button) => this.isButtonEnabled(button)).map((button) => button.innerText);
    }

    get buttonElements(): HTMLButtonElement[] {
        return Array.from(this.rootElement.querySelectorAll<HTMLButtonElement>('.mat-dialog-actions button'));
    }

    get hasLoadingIndicator(): boolean | undefined {
        const progressBarElement = this.rootElement.querySelector('.mat-progress-bar');
        return progressBarElement ? !progressBarElement.classList.contains('tsw-invisible') : undefined;
    }

    clickButton(label: string): void {
        const buttonToClick = this.buttonElements.find((button) => button.innerText === label);
        if (!buttonToClick) {
            throw new Error(`There is no button with label "${label}"`);
        }
        if (!this.isButtonEnabled(buttonToClick)) {
            throw new Error(`The button with label "${label}" is disabled`);
        }
        fakeAsync(() => {
            buttonToClick.click();
            this.fixture.detectChanges();
            tick();
            this.fixture.detectChanges();
        })();
    }

    private isButtonEnabled(button: HTMLButtonElement) {
        return !button.hasAttribute('disabled') && !button.classList.contains('mat-button-disabled');
    }
}
