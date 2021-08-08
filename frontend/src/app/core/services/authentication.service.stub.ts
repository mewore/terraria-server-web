import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { AuthenticatedUser } from '../types';
import { AuthenticationService } from './authentication.service';

@Injectable()
export class AuthenticationServiceStub implements AuthenticationService {
    public get currentUser(): AuthenticatedUser | undefined {
        return undefined;
    }

    public get canManageHosts(): boolean {
        return true;
    }

    public get canManageTerraria(): boolean {
        return true;
    }

    public get userObservable(): Observable<AuthenticatedUser | undefined> {
        throw EMPTY;
    }

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
