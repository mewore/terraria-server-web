import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CreateTerrariaInstanceDialogComponent } from './create-terraria-instance-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class CreateTerrariaInstanceDialogService {
    constructor(private readonly dialog: MatDialog) {}

    openDialog(): Promise<void> {
        return this.dialog.open(CreateTerrariaInstanceDialogComponent, {
            maxWidth: '40em',
        }).afterClosed().toPromise();
    }
}
