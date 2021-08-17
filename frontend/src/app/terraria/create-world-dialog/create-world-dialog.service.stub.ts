import { Injectable } from '@angular/core';
import { TerrariaInstanceEntity } from 'src/generated/backend';
import { CreateWorldDialogInput } from './create-world-dialog.component';
import { CreateWorldDialogService } from './create-world-dialog.service';

@Injectable()
export class CreateWorldDialogServiceStub implements CreateWorldDialogService {
    openDialog(_data: CreateWorldDialogInput): Promise<TerrariaInstanceEntity | undefined> {
        throw new Error('Method not mocked.');
    }
}
