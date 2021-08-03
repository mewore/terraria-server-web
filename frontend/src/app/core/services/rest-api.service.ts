import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpClient as GeneratedHttpClient, RestApplicationClient, RestResponse } from 'src/generated/backend';

@Injectable({
    providedIn: 'root',
})
export class RestApiService extends RestApplicationClient {
    constructor(httpClient: HttpClient) {
        super(new HttpClientAdapter(httpClient));
    }
}

class HttpClientAdapter implements GeneratedHttpClient {
    constructor(private readonly http: HttpClient) {}

    request<R>(requestConfig: RequestConfig<R>): RestResponse<R> {
        return this.http
            .request<R>(requestConfig.method, '/' + requestConfig.url, {
                body: requestConfig.data,
                params: requestConfig.queryParams,
                responseType: 'json',
            })
            .toPromise();
    }
}

interface RequestConfig<R> {
    method: string;
    url: string;
    queryParams?: any;
    data?: any;
    copyFn?: ((data: R) => R) | undefined;
}
