import { Injectable } from '@angular/core';
import { AuthenticatedUser } from '../types';

export class StorageService {
    user?: AuthenticatedUser;
}

@Injectable({
    providedIn: 'root',
})
export class StorageServiceImpl implements StorageService {
    private readonly USER_STORAGE_KEY = 'user';

    constructor() {}

    get user(): AuthenticatedUser | undefined {
        const rawUser = sessionStorage.getItem(this.USER_STORAGE_KEY);
        const user = rawUser ? (JSON.parse(rawUser) as AuthenticatedUser) : undefined;
        return user?.authData ? user : undefined;
    }

    set user(user: AuthenticatedUser | undefined) {
        if (user) {
            sessionStorage.setItem(this.USER_STORAGE_KEY, JSON.stringify(user));
        } else {
            sessionStorage.removeItem(this.USER_STORAGE_KEY);
        }
    }
}
