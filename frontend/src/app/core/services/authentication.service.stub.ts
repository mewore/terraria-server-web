import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { AuthenticatedUser } from '../types';
import { AuthenticationService } from './authentication.service';

@Injectable()
export class AuthenticationServiceStub implements AuthenticationService {
    readonly currentUser: AuthenticatedUser | undefined;
    readonly canManageHosts: boolean = false;
    readonly userObservable: Observable<AuthenticatedUser | undefined> = EMPTY;

    logIn(_username: string, _password: string): Promise<AuthenticatedUser> {
        throw new Error('Method not mocked.');
    }

    signUp(_username: string, _password: string): Promise<AuthenticatedUser> {
        throw new Error('Method not mocked.');
    }

    logOut(): Promise<void> {
        throw new Error('Method not mocked.');
    }
}
