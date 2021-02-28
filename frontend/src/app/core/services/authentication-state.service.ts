import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export enum SessionState {
    UNAUTHENTICATED,
    UNSURE,
    CHECKING,
    AUTHENTICATED,
}

@Injectable({
    providedIn: 'root',
})
export class AuthenticationStateService {
    private unsureSubject = new BehaviorSubject<void>(undefined);

    public unsureObservable = this.unsureSubject.asObservable();

    private privateAuthData?: string;

    sessionState: SessionState = SessionState.UNAUTHENTICATED;

    constructor() {}

    get authData(): string | undefined {
        return this.privateAuthData;
    }

    set authData(authData: string | undefined) {
        this.privateAuthData = authData;
        this.sessionState = authData ? SessionState.AUTHENTICATED : SessionState.UNAUTHENTICATED;
    }

    markAsUnsure(): void {
        if (this.sessionState === SessionState.AUTHENTICATED) {
            this.sessionState = SessionState.UNSURE;
            this.unsureSubject.next(undefined);
        }
    }
}
