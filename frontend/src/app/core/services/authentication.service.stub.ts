import { EMPTY, Observable } from 'rxjs';
import { AuthenticatedUser } from '../types';
import { AuthenticationService } from './authentication.service';

export class AuthenticationServiceStub implements Required<AuthenticationService> {
    readonly userObservable: Observable<AuthenticatedUser | undefined> = EMPTY;

    get currentUser(): AuthenticatedUser | undefined {
        throw new Error('Method not mocked.');
    }

    get canManageHosts(): boolean {
        throw new Error('Method not mocked.');
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
