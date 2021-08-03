import { TestBed } from '@angular/core/testing';
import { AuthenticatedUser } from '../types';

import { StorageService, StorageServiceImpl } from './storage.service';

describe('StorageService', () => {
    let service: StorageService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(StorageServiceImpl);
    });

    afterEach(() => {
        sessionStorage.clear();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('when there is no data', () => {
        beforeEach(() => {
            sessionStorage.clear();
        });

        it('should have no user', () => {
            expect(service.user).toBeUndefined();
        });
    });

    describe('when there is user data', () => {
        let user: AuthenticatedUser;

        beforeEach(() => {
            user = {
                authData: 'authData',
                sessionToken: 'token',
                username: 'username',
            };
            sessionStorage.setItem('user', JSON.stringify(user));
        });

        it('should have a user', () => {
            expect(service.user).toEqual(user);
        });

        describe('setting the user to undefined', () => {
            beforeEach(() => {
                service.user = undefined;
            });

            it('should erase the user', () => {
                expect(sessionStorage.getItem('user')).toBeNull();
            });
        });
    });

    describe('when there is user data without authData', () => {
        let user: Omit<AuthenticatedUser, 'authData'>;

        beforeEach(() => {
            user = {
                sessionToken: 'token',
                username: 'username',
            };
            sessionStorage.setItem('user', JSON.stringify(user));
        });

        it('should have no user', () => {
            expect(service.user).toBeUndefined();
        });
    });

    describe('setting the user', () => {
        beforeEach(() => {
            service.user = {
                authData: 'authData',
                sessionToken: 'token',
                username: 'username',
            };
        });

        it('should save the user in the storage', () => {
            const rawUser = sessionStorage.getItem('user');
            expect(rawUser).toBeDefined();
            expect(rawUser ? JSON.parse(rawUser) : undefined).toEqual({
                authData: 'authData',
                sessionToken: 'token',
                username: 'username',
            } as AuthenticatedUser);
        });
    });
});
