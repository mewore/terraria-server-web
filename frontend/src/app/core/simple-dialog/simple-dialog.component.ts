import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface ButtonDefinition<T> {
    labelKey: string;
    onClicked: () => Promise<T> | T | undefined;
}

export interface SimpleDialogInput<T> {
    titleKey: string;
    descriptionKey?: string;
    primaryButton: ButtonDefinition<T>;
    extraButtons?: ButtonDefinition<T>[];
    warn?: boolean;
}

@Component({
    selector: 'tsw-simple-dialog',
    templateUrl: './simple-dialog.component.html',
    styleUrls: ['./simple-dialog.component.sass'],
})
export class SimpleDialogComponent<T> {
    loading = false;

    constructor(
        private readonly dialog: MatDialogRef<SimpleDialogComponent<T>, T>,
        @Inject(MAT_DIALOG_DATA) readonly data: SimpleDialogInput<T>
    ) {}

    async onButtonClicked(button: ButtonDefinition<T>): Promise<void> {
        this.loading = true;
        try {
            const result = button.onClicked();
            this.dialog.close(result instanceof Promise ? await result : result);
        } finally {
            this.loading = false;
        }
    }
}
