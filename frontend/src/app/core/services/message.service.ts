import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs/esm6/i-message';
import { OperatorFunction } from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as SockJS from 'sockjs-client';
import {
    HostEntity,
    TerrariaInstanceEntity,
    TerrariaInstanceEventMessage,
    TerrariaInstanceMessage,
    TerrariaWorldEntity,
} from 'src/generated/backend';
import { StompService } from './stomp.service';

export abstract class MessageService {
    abstract watchHostInstanceCreation(host: HostEntity): Observable<TerrariaInstanceEntity>;
    abstract watchInstanceChanges(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceMessage>;
    abstract watchInstanceDeletion(instance: TerrariaInstanceEntity): Observable<void>;
    abstract watchInstanceEvents(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceEventMessage>;
    abstract watchWorldDeletion(world: TerrariaWorldEntity): Observable<void>;
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
            reconnectDelay: 20000,
        });
        this.rxStomp.activate();
    }

    private static messageParser<T>(): OperatorFunction<IMessage, T> {
        return map((message: IMessage): T => JSON.parse(message.body));
    }

    watchHostInstanceCreation(host: HostEntity): Observable<TerrariaInstanceEntity> {
        return this.rxStomp.watch(`/topic/hosts/${host.id}/instances`).pipe(MessageServiceImpl.messageParser());
    }

    watchInstanceChanges(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceMessage> {
        return this.rxStomp.watch(`/topic/instances/${instance.id}`).pipe(MessageServiceImpl.messageParser());
    }

    watchInstanceDeletion(instance: TerrariaInstanceEntity): Observable<void> {
        return this.rxStomp.watch(`/topic/instances/${instance.id}/deletion`).pipe(map(() => undefined));
    }

    watchInstanceEvents(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceEventMessage> {
        return this.rxStomp.watch(`/topic/instances/${instance.id}/events`).pipe(MessageServiceImpl.messageParser());
    }

    watchWorldDeletion(world: TerrariaWorldEntity): Observable<void> {
        return this.rxStomp.watch(`/topic/worlds/${world.id}/deletion`).pipe(map(() => undefined));
    }

    ngOnDestroy(): void {
        this.rxStomp.deactivate();
    }
}
