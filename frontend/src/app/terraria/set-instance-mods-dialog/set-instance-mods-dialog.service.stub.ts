import { Injectable } from '@angular/core';
import { TerrariaInstanceEntity } from 'src/generated/backend';
import { SetInstanceModsDialogService } from './set-instance-mods-dialog.service';

@Injectable()
export class SetInstanceModsDialogServiceStub implements SetInstanceModsDialogService {
    openDialog(data: TerrariaInstanceEntity): Promise<TerrariaInstanceEntity | undefined> {
        throw new Error('Method not mocked.');
    }
}
