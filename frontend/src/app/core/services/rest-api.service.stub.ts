import { Injectable } from '@angular/core';
import { HttpClientStub } from 'src/stubs/http-client.stub';
import { RestApiService } from './rest-api.service';

/**
 * The stub of {@link RestApiService}. It extends {@link RestApiService} because its methods are
 * generated automatically so keeping track of them would be difficult.
 */
@Injectable()
export class RestApiServiceStub extends RestApiService {
    constructor() {
        super(new HttpClientStub().masked());
    }
}
