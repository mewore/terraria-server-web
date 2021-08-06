import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { StompService } from './stomp.service';

@Injectable()
export class StompServiceStub implements StompService {
    createStompClient(): RxStomp {
        throw new Error('Method not mocked.');
    }
}
