import { Injectable } from '@angular/core';
import { AuthenticatedUser } from 'src/app/core/types';
import { AuthenticationDialogService } from './authentication-dialog.service';

@Injectable()
export class AuthenticationDialogServiceStub implements AuthenticationDialogService {
    openDialog(): Promise<AuthenticatedUser | undefined> {
        throw new Error('Method not mocked.');
    }
}
