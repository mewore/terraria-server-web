import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { HostEntity } from 'src/generated/backend';
import {
    CreateTerrariaInstanceDialogComponent,
    CreateTerrariaInstanceDialogInput,
    CreateTerrariaInstanceDialogOutput,
} from './create-terraria-instance-dialog.component';

export abstract class CreateTerrariaInstanceDialogService {
    abstract openDialog(host: HostEntity): Promise<CreateTerrariaInstanceDialogOutput | undefined>;
}

@Injectable({
    providedIn: 'root',
})
export class CreateTerrariaInstanceDialogServiceImpl implements CreateTerrariaInstanceDialogService {
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
