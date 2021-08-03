import { Injectable } from '@angular/core';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';
import { CreateTerrariaInstanceDialogService } from './create-terraria-instance-dialog.service';

@Injectable()
export class CreateTerrariaInstanceDialogServiceStub implements CreateTerrariaInstanceDialogService {
    openDialog(_host: HostEntity): Promise<TerrariaInstanceEntity | undefined> {
        throw new Error('Method not mocked.');
    }
}
