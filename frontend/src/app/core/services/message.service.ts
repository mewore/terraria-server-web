import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs/esm6/i-message';
import { OperatorFunction } from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as SockJS from 'sockjs-client';
import { TerrariaInstanceEntity, TerrariaInstanceEventMessage, TerrariaInstanceMessage } from 'src/generated/backend';
import { StompService } from './stomp.service';

export abstract class MessageService {
    abstract watchInstanceChanges(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceMessage>;
    abstract watchInstanceEvents(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceEventMessage>;
}

@Injectable({
    providedIn: 'root',
})
export class MessageServiceImpl implements MessageService, OnDestroy {
    private readonly rxStomp: RxStomp;

    constructor(stompService: StompService) {
        this.rxStomp = stompService.createStompClient();

        this.rxStomp.configure({
            webSocketFactory: () => new SockJS('/messages'),
            connectHeaders: {
                login: 'guest',
                passcode: 'guest',
            },
            heartbeatIncoming: 0,
            heartbeatOutgoing: 20000,
            reconnectDelay: 2000,
            debug: (msg: string): void => {
                console.log(new Date(), msg);
            },
        });
        this.rxStomp.activate();
    }

    private static messageParser<T>(): OperatorFunction<IMessage, T> {
        return map((message: IMessage): T => JSON.parse(message.body));
    }

    public watchInstanceChanges(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceMessage> {
        return this.rxStomp
            .watch(`/topic/instances/${instance.id}`)
            .pipe(MessageServiceImpl.messageParser());
    }

    public watchInstanceEvents(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceEventMessage> {
        return this.rxStomp
            .watch(`/topic/instances/${instance.id}/events`)
            .pipe(MessageServiceImpl.messageParser());
    }

    ngOnDestroy(): void {
        this.rxStomp.deactivate();
    }
}
