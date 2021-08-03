import { Injectable } from '@angular/core';

export abstract class ErrorService {
    abstract showError(error: Error): void;
}

@Injectable({
    providedIn: 'root',
})
export class ErrorServiceImpl implements ErrorService {
    showError(error: Error): void {
        console.error(error);
    }
}
