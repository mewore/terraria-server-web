import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ButtonDefinition, SimpleDialogComponent, SimpleDialogInput } from './simple-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class SimpleDialogService {
    public static readonly OK_BUTTON = {} as ButtonDefinition<void>;

    constructor(private readonly dialog: MatDialog) {}

    openDialog<T>(data: SimpleDialogInput<T>): Promise<T | undefined> {
        return this.dialog
            .open<SimpleDialogComponent<T>, SimpleDialogInput<T>, T>(SimpleDialogComponent, {
                data,
                maxWidth: '30em',
                minWidth: '20em',
                width: '80%',
            })
            .afterClosed()
            .toPromise();
    }
}
