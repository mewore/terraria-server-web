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

    private static queryParamsToFormData(params: { [key: string]: any }): FormData {
        const formData = new FormData();
        for (const [key, param] of Object.entries(params)) {
            for (const value of [param].flat()) {
                value instanceof File ? formData.append(key, value, value.name) : formData.append(key, `${value}`);
            }
        }
        return formData;
    }

    request<R>(requestConfig: RequestConfig<R>): RestResponse<R> {
        requestConfig.url = '/' + requestConfig.url;

        const isUploadingFile =
            !requestConfig.data &&
            requestConfig.queryParams &&
            Object.values(requestConfig.queryParams)
                .flat()
                .some((value) => value instanceof File);
        if (isUploadingFile) {
            requestConfig.data = HttpClientAdapter.queryParamsToFormData(requestConfig.queryParams);
            requestConfig.queryParams = undefined;
        }

        return this.http
            .request<R>(requestConfig.method, requestConfig.url, {
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
