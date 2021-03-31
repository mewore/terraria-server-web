import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';
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

    openDialog(host: HostEntity): Promise<CreateTerrariaInstanceDialogOutput | undefined> {
        return this.dialog
            .open<
                CreateTerrariaInstanceDialogComponent,
                CreateTerrariaInstanceDialogInput,
                CreateTerrariaInstanceDialogOutput
            >(CreateTerrariaInstanceDialogComponent, {
                data: host,
                maxWidth: '50em',
                minWidth: '20em',
                width: '80%',
            })
            .afterClosed()
            .toPromise();
    }
}
