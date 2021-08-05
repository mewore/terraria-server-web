import { Injectable } from '@angular/core';
import { TerrariaInstanceEntity } from 'src/generated/backend';
import { RunServerDialogInput } from './run-server-dialog.component';
import { RunServerDialogService } from './run-server-dialog.service';

@Injectable()
export class RunServerDialogServiceStub implements RunServerDialogService {
    openDialog(data: RunServerDialogInput): Promise<TerrariaInstanceEntity | undefined> {
        throw new Error('Method not mocked.');
    }
}
