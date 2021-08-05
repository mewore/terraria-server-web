import { Injectable } from '@angular/core';
import { SimpleDialogInput } from './simple-dialog.component';
import { SimpleDialogService } from './simple-dialog.service';

@Injectable()
export class SimpleDialogServiceStub implements SimpleDialogService {
    openDialog<T>(data: SimpleDialogInput<T>): Promise<T | undefined> {
        throw new Error('Method not mocked.');
    }
}
