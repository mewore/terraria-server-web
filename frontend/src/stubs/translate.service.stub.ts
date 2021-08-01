import { TranslateService } from '@ngx-translate/core';
import { EMPTY, Observable } from 'rxjs';
import { ImperfectStub as ImperfectStub } from './imperfect-stub';

export class TranslateServiceStub extends ImperfectStub<TranslateService> {
    addLangs(_langs: Array<string>): void {}

    setDefaultLang(_lang: string): void {}

    getBrowserLang(): string {
        return 'en-US';
    }

    use(_lang: string): Observable<any> {
        return EMPTY;
    }
}
