import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HostEntity } from 'src/generated/backend';
import { HttpClientStub } from 'src/stubs/http-client.stub';

import { RestApiService } from './rest-api.service';

describe('RestApiService', () => {
    let service: RestApiService;

    let httpClient: HttpClient;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [{ provide: HttpClient, useClass: HttpClientStub }],
        });
        httpClient = TestBed.inject(HttpClient);
        service = TestBed.inject(RestApiService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getHost', () => {
        let requestSpy: jasmine.Spy<(method: string, url: string, options: any) => Observable<any>>;
        let returnedHost: HostEntity;
        let result: HostEntity;

        beforeEach(async () => {
            returnedHost = {} as HostEntity;
            requestSpy = spyOn(httpClient, 'request').and.returnValue(of(returnedHost));
            result = await service.getHost(1);
        });

        it('should make the expected request', () => {
            expect(requestSpy).toHaveBeenCalledWith('GET', '/api/hosts/1', {
                body: undefined,
                params: undefined,
                responseType: 'json',
            });
        });

        it('should return the received host', () => {
            expect(result).toBe(returnedHost);
        });
    });
});
