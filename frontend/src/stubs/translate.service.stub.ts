import { TranslateService } from '@ngx-translate/core';
import { EMPTY, Observable, of } from 'rxjs';
import { ImperfectStub as ImperfectStub } from './imperfect-stub';

import * as i18nFileRoot from '../assets/i18n/en-US.json';

interface I18nFileNode {
    [key: string]: I18nFileNode | string | undefined;
}

export class TranslateServiceStub extends ImperfectStub<TranslateService> {
    addLangs(_langs: Array<string>): void {}

    setDefaultLang(_lang: string): void {}

    getBrowserLang(): string {
        return 'en-US';
    }

    use(_lang: string): Observable<any> {
        return EMPTY;
    }

    get(key: string | string[], interpolateParams?: { [key: string]: any }): Observable<string> {
        return of(this.instant(key, interpolateParams));
    }

    stream(key: string | Array<string>, interpolateParams?: { [key: string]: any }): Observable<string> {
        return this.get(key, interpolateParams);
    }

    instant(key: string | string[], interpolateParams?: { [key: string]: any }): string {
        const keyParts = typeof key === 'string' ? key.split('.') : key;
        const root = i18nFileRoot as unknown as { default?: I18nFileNode | string };
        if (!root.default) {
            throw new Error(`There is no 'default' property!`);
        }
        let i18nNode = root.default;
        for (let i = 0; i < keyParts.length; i++) {
            if (typeof i18nNode === 'string') {
                throw new Error(
                    `There is no ${this.joinKeyParts(keyParts)} i18n node because ${this.joinKeyParts(
                        keyParts.slice(0, i)
                    )} ("${i18nNode}") is already a leaf node`
                );
            }
            const nextNode = i18nNode[keyParts[i]];
            if (!nextNode) {
                throw new Error(`The i18n key ${this.joinKeyParts(keyParts.slice(0, i + 1))} does not exist`);
            }
            i18nNode = nextNode;
        }
        if (typeof i18nNode !== 'string') {
            throw new Error(`The '${this.joinKeyParts(keyParts)}' i18n node is not a leaf node`);
        }
        if (!interpolateParams) {
            return i18nNode;
        }
        const interpolationMap = new Map<string, any>(Object.entries(interpolateParams));
        for (const interpolationKey of interpolationMap.keys()) {
            const replaceString = `{{ ${interpolationKey} }}`;
            if (!i18nNode.includes(replaceString)) {
                console.warn(
                    `The i18n node ${this.joinKeyParts(keyParts)} ("${i18nNode}") does not contain "${replaceString}"`
                );
            }
        }
        return i18nNode.replace(
            /\{\{\s*(\S+)\s*\}\}/g,
            (wholeMatch: string, interpolationKey: string, _index: number) => {
                return interpolationMap.has(interpolationKey) ? interpolationMap.get(interpolationKey) : wholeMatch;
            }
        );
    }

    private joinKeyParts(keyParts: string[]): string {
        return keyParts.some((part) => part.includes('.')) ? JSON.stringify(keyParts) : `[${keyParts.join('.')}]`;
    }
}
