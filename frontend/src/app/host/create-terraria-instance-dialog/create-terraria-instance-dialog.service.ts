import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { HostEntity } from 'src/generated/backend';
import {
    CreateTerrariaInstanceDialogComponent,
    CreateTerrariaInstanceDialogInput,
    CreateTerrariaInstanceDialogOutput,
} from './create-terraria-instance-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class CreateTerrariaInstanceDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(host: HostEntity): Promise<void> {
        return this.dialog
            .open<
                CreateTerrariaInstanceDialogComponent,
                CreateTerrariaInstanceDialogInput,
                CreateTerrariaInstanceDialogOutput
            >(CreateTerrariaInstanceDialogComponent, {
                data: host,
                maxWidth: '40em',
            })
            .afterClosed()
            .toPromise();
    }
}
