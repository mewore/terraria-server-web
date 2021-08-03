import { HttpClient } from '@angular/common/http';
import { ImperfectStub } from './imperfect-stub';

export class HttpClientStub extends ImperfectStub<HttpClient> {
    request(): never {
        throw new Error('Method not mocked.');
    }

    delete(): never {
        throw new Error('Method not mocked.');
    }

    get(): never {
        throw new Error('Method not mocked.');
    }

    head(): never {
        throw new Error('Method not mocked.');
    }

    jsonp(): never {
        throw new Error('Method not mocked.');
    }

    options(): never {
        throw new Error('Method not mocked.');
    }

    patch(): never {
        throw new Error('Method not mocked.');
    }

    post(): never {
        throw new Error('Method not mocked.');
    }

    put(): never {
        throw new Error('Method not mocked.');
    }
}
