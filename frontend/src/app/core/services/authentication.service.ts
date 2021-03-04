import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { SessionViewModel } from 'src/generated/backend';
import { AuthenticatedUser } from '../types';
import { AuthenticationStateService, SessionState } from './authentication-state.service';
import { RestApiService } from './rest-api.service';

@Injectable({
    providedIn: 'root',
})
export class AuthenticationService {
    private readonly SESSION_STORAGE_KEY = 'user';

    private userSubject: BehaviorSubject<AuthenticatedUser | undefined>;

    userObservable: Observable<AuthenticatedUser | undefined>;

    constructor(
        private readonly restApi: RestApiService,
        private readonly authenticationStateService: AuthenticationStateService
    ) {
        const rawUser = sessionStorage.getItem(this.SESSION_STORAGE_KEY);
        const initialUser = rawUser ? JSON.parse(rawUser) : undefined;
        this.userSubject = new BehaviorSubject<AuthenticatedUser | undefined>(initialUser);
        this.userObservable = this.userSubject.asObservable();
        this.userObservable.subscribe((newAuth) => {
            if (newAuth) {
                sessionStorage.setItem(this.SESSION_STORAGE_KEY, JSON.stringify(newAuth));
                this.authenticationStateService.authData = newAuth.authData;
            } else {
                sessionStorage.removeItem(this.SESSION_STORAGE_KEY);
                this.authenticationStateService.authData = undefined;
            }
        });
        this.authenticationStateService.unsureObservable.subscribe({
            next: () => {
                if (this.currentUser && this.authenticationStateService.sessionState === SessionState.UNSURE) {
                    this.refreshState(this.currentUser);
                }
            },
        });
        if (initialUser && initialUser.authData) {
            this.authenticationStateService.authData = initialUser.authData;
            this.authenticationStateService.markAsUnsure();
        }
    }

    private async refreshState(currentUser: AuthenticatedUser): Promise<void> {
        this.authenticationStateService.sessionState = SessionState.CHECKING;
        try {
            const accountType = await this.restApi.ping();
            this.userSubject.next({
                authData: currentUser.authData,
                sessionToken: currentUser.sessionToken,
                username: currentUser.username,
                accountType,
            });
            this.authenticationStateService.sessionState = SessionState.AUTHENTICATED;
        } catch (error) {
            this.userSubject.next(undefined);
            this.authenticationStateService.sessionState = SessionState.UNAUTHENTICATED;
            throw error;
        }
    }

    get currentUser(): AuthenticatedUser | undefined {
        return this.userSubject.value;
    }

    async logIn(username: string, password: string): Promise<AuthenticatedUser> {
        const session = await this.restApi.logIn({ username, password });
        return this.saveSesssion(username, session);
    }

    async signUp(username: string, password: string): Promise<AuthenticatedUser> {
        const session = await this.restApi.signUp({ username, password });
        return this.saveSesssion(username, session);
    }

    private saveSesssion(username: string, session: SessionViewModel): AuthenticatedUser {
        const result: AuthenticatedUser = {
            username,
            sessionToken: session.token,
            authData: this.encodeBasicAuth(username, session.token),
            accountType: session.role,
        };
        this.userSubject.next(result);
        return result;
    }

    private encodeBasicAuth(username: string, token: string): string {
        return 'Basic ' + btoa(username + ':' + token);
    }

    async logOut(): Promise<void> {
        await this.restApi.logOut();
        this.userSubject.next(undefined);
    }

    canManageAccounts(): boolean {
        return this.currentUser?.accountType?.manageAccounts || false;
    }
}
