import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, Subscription } from 'rxjs';
import { SessionViewModel } from 'src/generated/backend';
import { AuthenticatedUser } from '../types';
import { AuthenticationStateService, SessionState } from './authentication-state.service';
import { ErrorService } from './error.service';
import { RestApiService } from './rest-api.service';
import { StorageService } from './storage.service';

export abstract class AuthenticationService {
    abstract readonly userObservable: Observable<AuthenticatedUser | undefined>;
    abstract readonly currentUser: AuthenticatedUser | undefined;
    abstract readonly canManageHosts: boolean;
    abstract readonly canManageTerraria: boolean;

    abstract logIn(username: string, password: string): Promise<AuthenticatedUser>;

    abstract signUp(username: string, password: string): Promise<AuthenticatedUser>;

    abstract logOut(): Promise<void>;
}

@Injectable({
    providedIn: 'root',
})
export class AuthenticationServiceImpl implements AuthenticationService, OnDestroy {
    readonly userObservable: Observable<AuthenticatedUser | undefined>;

    currentUser: AuthenticatedUser | undefined;

    private readonly userSubject = new Subject<AuthenticatedUser | undefined>();

    private readonly unsureSubscription: Subscription;

    constructor(
        private readonly restApi: RestApiService,
        private readonly authenticationStateService: AuthenticationStateService,
        private readonly storageService: StorageService,
        private readonly errorService: ErrorService
    ) {
        this.userObservable = this.userSubject.asObservable();
        const initialUser = storageService.user;
        this.unsureSubscription = this.authenticationStateService.unsureObservable.subscribe({
            next: () => {
                if (this.currentUser) {
                    this.refreshState(this.currentUser);
                }
            },
        });
        if (initialUser && initialUser.authData) {
            this.currentUser = initialUser;
            this.authenticationStateService.authData = initialUser.authData;
            this.authenticationStateService.markAsUnsure();
        }
    }

    ngOnDestroy(): void {
        this.unsureSubscription.unsubscribe();
    }

    get canManageHosts(): boolean {
        return this.currentUser?.accountType?.ableToManageHosts || false;
    }

    get canManageTerraria(): boolean {
        return this.currentUser?.accountType?.ableToManageTerraria || false;
    }

    async logIn(username: string, password: string): Promise<AuthenticatedUser> {
        const session = await this.restApi.logIn({ username, password });
        return this.saveSession(username, session);
    }

    async signUp(username: string, password: string): Promise<AuthenticatedUser> {
        const session = await this.restApi.signUp({ username, password });
        return this.saveSession(username, session);
    }

    async logOut(): Promise<void> {
        await this.restApi.logOut();
        this.setCurrentUser(undefined);
    }

    private async refreshState(currentUser: AuthenticatedUser): Promise<void> {
        this.authenticationStateService.sessionState = SessionState.CHECKING;
        try {
            const accountType = await this.restApi.ping();
            this.setCurrentUser({
                authData: currentUser.authData,
                sessionToken: currentUser.sessionToken,
                username: currentUser.username,
                accountType: accountType || undefined,
            });
            this.authenticationStateService.sessionState = SessionState.AUTHENTICATED;
        } catch (error) {
            this.setCurrentUser(undefined);
            this.authenticationStateService.sessionState = SessionState.UNAUTHENTICATED;
            this.errorService.showError(error);
        }
    }

    private setCurrentUser(value: AuthenticatedUser | undefined): void {
        this.currentUser = value;
        this.userSubject.next(value);
        this.storageService.user = value;
        this.authenticationStateService.authData = value?.authData;
    }

    private saveSession(username: string, session: SessionViewModel): AuthenticatedUser {
        const newUser: AuthenticatedUser = {
            username,
            sessionToken: session.token,
            authData: this.encodeBasicAuth(username, session.token),
            accountType: session.role || undefined,
        };
        this.setCurrentUser(newUser);
        return newUser;
    }

    private encodeBasicAuth(username: string, token: string): string {
        return 'Basic ' + btoa(username + ':' + token);
    }
}
