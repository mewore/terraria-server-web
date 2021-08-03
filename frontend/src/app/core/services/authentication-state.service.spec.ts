import { TestBed } from '@angular/core/testing';
import { Subscription } from 'rxjs';

import { AuthenticationStateService, AuthenticationStateServiceImpl, SessionState } from './authentication-state.service';

describe('AuthenticationStateService', () => {
    let service: AuthenticationStateService;

    let unsureSubscription: Subscription;
    let unsureNotificationCount: number;

    beforeEach(() => {
        TestBed.configureTestingModule({});

        service = TestBed.inject(AuthenticationStateServiceImpl);

        unsureNotificationCount = 0;
        unsureSubscription = service.unsureObservable.subscribe({
            next: () => unsureNotificationCount++,
        });
    });

    afterEach(() => {
        unsureSubscription.unsubscribe();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should have no unsure notifications in the beginning', () => {
        expect(unsureNotificationCount).toBe(0);
    });

    it('should be unauthenticated', () => {
        expect(service.sessionState).toBe(SessionState.UNAUTHENTICATED);
    });

    describe('when the auth data is set to something', () => {
        beforeEach(() => {
            service.authData = 'authData';
        });

        it('should save the auth data', () => {
            expect(service.authData).toBe('authData');
        });

        it('should become authenticated', () => {
            expect(service.sessionState).toBe(SessionState.AUTHENTICATED);
        });

        describe('when the auth data is unset', () => {
            beforeEach(() => {
                service.authData = undefined;
            });

            it('should become unauthenticated', () => {
                expect(service.sessionState).toBe(SessionState.UNAUTHENTICATED);
            });
        });

        describe('when marked as unsure while authenticated', () => {
            beforeEach(() => {
                service.markAsUnsure();
            });

            it('should become unsure', () => {
                expect(service.sessionState).toBe(SessionState.UNSURE);
            });

            it('should notify once about being unsure', () => {
                expect(unsureNotificationCount).toBe(1);
            });
        });
    });

    describe('when marked as unsure while unauthenticated', () => {
        beforeEach(() => {
            service.markAsUnsure();
        });

        it('should stay unauthenticated', () => {
            expect(service.sessionState).toBe(SessionState.UNAUTHENTICATED);
        });
    });
});
