import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthenticationStateService, SessionState } from './authentication-state.service';

@Injectable()
export class AuthenticationStateServiceStub implements AuthenticationStateService {
    get unsureObservable(): Observable<void> {
        throw new Error('Method not mocked.');
    }

    sessionState: SessionState = SessionState.UNAUTHENTICATED;
    authData: string | undefined;

    markAsUnsure(): void {
        throw new Error('Method not mocked.');
    }
}
