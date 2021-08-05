import { Injectable } from '@angular/core';

export abstract class ErrorService {
    abstract showError(error: Error | string): void;
}

@Injectable({
    providedIn: 'root',
})
export class ErrorServiceImpl implements ErrorService {
    showError(error: Error | string): void {
        console.error(error);
    }
}
