import { Injectable } from '@angular/core';
import { LogOutDialogComponentOutput } from './log-out-dialog.component';
import { LogOutDialogService } from './log-out-dialog.service';

@Injectable()
export class LogOutDialogServiceStub implements LogOutDialogService {
    openDialog(): Promise<LogOutDialogComponentOutput> {
        throw new Error('Method not mocked.');
    }
}
