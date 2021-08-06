import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';

export abstract class StompService {
    abstract createStompClient(): RxStomp;
}

@Injectable({
    providedIn: 'root',
})
export class StompServiceImpl implements StompService {
    createStompClient(): RxStomp {
        return new RxStomp();
    }
}
