import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import {
    SetInstanceModsDialogComponent,
    SetInstanceModsDialogInput,
    SetInstanceModsDialogOutput,
} from './set-instance-mods-dialog.component';

export abstract class SetInstanceModsDialogService {
    abstract openDialog(data: SetInstanceModsDialogInput): Promise<SetInstanceModsDialogOutput | undefined>;
}

@Injectable({
    providedIn: 'root',
})
export class SetInstanceModsDialogServiceImpl implements SetInstanceModsDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(data: SetInstanceModsDialogInput): Promise<SetInstanceModsDialogOutput | undefined> {
        return this.dialog
            .open<SetInstanceModsDialogComponent, SetInstanceModsDialogInput, SetInstanceModsDialogOutput>(
                SetInstanceModsDialogComponent,
                {
                    data,
                    maxWidth: '50em',
                    minWidth: '20em',
                    width: '80%',
                }
            )
            .afterClosed()
            .toPromise();
    }
}
