import { IRxStompPublishParams, IWatchParams, RxStomp, RxStompConfig, RxStompState } from '@stomp/rx-stomp';
import { Client, IFrame, IMessage, StompHeaders } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { ImperfectStub } from './imperfect-stub';

export class RxStompStub extends ImperfectStub<RxStomp> {
    get connectionState$(): BehaviorSubject<RxStompState> {
        throw new Error('Method not mocked.');
    }
    get connected$(): Observable<RxStompState> {
        throw new Error('Method not mocked.');
    }
    get serverHeaders$(): Observable<StompHeaders> {
        throw new Error('Method not mocked.');
    }
    get unhandledMessage$(): Subject<IMessage> {
        throw new Error('Method not mocked.');
    }
    get unhandledFrame$(): Subject<IFrame> {
        throw new Error('Method not mocked.');
    }
    get unhandledReceipts$(): Subject<IFrame> {
        throw new Error('Method not mocked.');
    }
    get stompErrors$(): Subject<IFrame> {
        throw new Error('Method not mocked.');
    }
    get webSocketErrors$(): Subject<Event> {
        throw new Error('Method not mocked.');
    }
    get stompClient(): Client {
        throw new Error('Method not mocked.');
    }
    get active(): boolean {
        throw new Error('Method not mocked.');
    }

    configure(_rxStompConfig: RxStompConfig): void {
        throw new Error('Method not mocked.');
    }
    activate(): void {
        throw new Error('Method not mocked.');
    }
    deactivate(): Promise<void> {
        throw new Error('Method not mocked.');
    }
    connected(): boolean {
        throw new Error('Method not mocked.');
    }
    publish(_parameters: IRxStompPublishParams): void {
        throw new Error('Method not mocked.');
    }
    watch(_destinationOrOptions: string | IWatchParams, _headers?: any): Observable<IMessage> {
        throw new Error('Method not mocked.');
    }
    watchForReceipt(_receiptId: string, _callback: (frame: IFrame) => void): void {
        throw new Error('Method not mocked.');
    }
}
