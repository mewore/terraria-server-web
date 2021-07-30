import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';

export class TranslateServiceStub {

    addLangs(_langs: Array<string>): void {}

    setDefaultLang(_lang: string): void {}

    getBrowserLang(): string {
        return 'en-US';
    }

    use(_lang: string): Observable<any> {
        return EMPTY;
    }
}
