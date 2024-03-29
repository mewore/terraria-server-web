import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export enum SessionState {
    UNAUTHENTICATED,
    UNSURE,
    CHECKING,
    AUTHENTICATED,
}

export abstract class AuthenticationStateService {
    abstract readonly unsureObservable: Observable<void>;
    abstract sessionState: SessionState;
    abstract authData: string | undefined;

    abstract markAsUnsure(): void;
}

@Injectable({
    providedIn: 'root',
})
export class AuthenticationStateServiceImpl implements AuthenticationStateService {
    readonly unsureObservable: Observable<void>;

    sessionState: SessionState = SessionState.UNAUTHENTICATED;

    private unsureSubject = new Subject<void>();

    private privateAuthData?: string;

    constructor() {
        this.unsureObservable = this.unsureSubject.asObservable();
    }

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
