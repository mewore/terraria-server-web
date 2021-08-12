import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { IWatchParams, RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs/esm6/i-message';
import { BehaviorSubject, Subject } from 'rxjs';
import * as SockJS from 'sockjs-client';
import {
    HostEntity,
    TerrariaInstanceEntity,
    TerrariaInstanceEventMessage,
    TerrariaInstanceMessage,
} from 'src/generated/backend';
import { RxStompStub } from 'src/stubs/rx-stomp.stub';
import { MessageService, MessageServiceImpl } from './message.service';
import { StompService } from './stomp.service';
import { StompServiceStub } from './stomp.service.stub';

describe('MessageService', () => {
    let service: MessageService;

    let stompClient: RxStomp;
    let stompService: StompService;

    let configuration: RxStompConfig;
    let activateSpy: jasmine.Spy<() => void>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [{ provide: StompService, useClass: StompServiceStub }],
        });

        stompClient = new RxStompStub().masked();
        spyOn(stompClient, 'configure').and.callFake((newConfig: RxStompConfig) => {
            configuration = newConfig;
        });
        activateSpy = spyOn(stompClient, 'activate').and.returnValue();

        stompService = TestBed.inject(StompService);
        spyOn(stompService, 'createStompClient').and.returnValue(stompClient);

        service = TestBed.inject(MessageServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should configure and then activate the STOMP client', () => {
        expect(stompClient.configure).toHaveBeenCalledBefore(stompClient.activate);
    });

    describe('the WebSocket factory in the configuration', () => {
        it('should be defined', () => {
            expect(configuration.webSocketFactory).toBeDefined();
        });

        it('should create a SockJS object', () => {
            const sockJs: WebSocket = configuration.webSocketFactory ? configuration.webSocketFactory() : undefined;
            expect(sockJs instanceof SockJS).toBeTrue();
            sockJs.close();
        });
    });

    describe('the debug callback in the configuration', () => {
        it('should be undefined', () => {
            expect(configuration.debug).toBeUndefined();
        });
    });

    describe('ngOnDestroy', () => {
        let deactivateSpy: jasmine.Spy<() => Promise<void>>;

        beforeEach(() => {
            deactivateSpy = spyOn(stompClient, 'deactivate').and.resolveTo();
            (service as MessageServiceImpl).ngOnDestroy();
        });

        it('should deactivate the STOMP client', () => {
            expect(deactivateSpy).toHaveBeenCalled();
        });
    });

    describe('watchHostInstanceCreation', () => {
        let watchSpy: jasmine.Spy<(destinationOrOptions: string | IWatchParams, headers?: any) => Subject<IMessage>>;
        let result: TerrariaInstanceEntity;
        const sentMessage = { id: 1 } as TerrariaInstanceEntity;

        beforeEach(fakeAsync(() => {
            watchSpy = spyOn(stompClient, 'watch').and.returnValue(
                new BehaviorSubject<IMessage>({ body: JSON.stringify(sentMessage) } as IMessage).asObservable()
            );
            const subscription = service.watchHostInstanceCreation({ id: 8 } as HostEntity).subscribe({
                next: (message) => (result = message),
            });
            tick(1000);
            subscription.unsubscribe();
        }));

        it('should watch for instance changes at the correct destination', () => {
            expect(watchSpy).toHaveBeenCalledOnceWith('/topic/hosts/8/instances');
        });

        it('should track the instance messages', () => {
            expect(result).toEqual(sentMessage);
        });
    });

    describe('watchInstanceChanges', () => {
        let watchSpy: jasmine.Spy<(destinationOrOptions: string | IWatchParams, headers?: any) => Subject<IMessage>>;
        let result: TerrariaInstanceMessage;
        const sentMessage: TerrariaInstanceMessage = {
            state: 'BOOTING_UP',
            options: { 1: 'option' },
            currentAction: 'BOOT_UP',
            pendingAction: 'RUN_SERVER',
        };

        beforeEach(fakeAsync(() => {
            watchSpy = spyOn(stompClient, 'watch').and.returnValue(
                new BehaviorSubject<IMessage>({ body: JSON.stringify(sentMessage) } as IMessage).asObservable()
            );
            const subscription = service.watchInstanceChanges({ id: 8 } as TerrariaInstanceEntity).subscribe({
                next: (message) => (result = message),
            });
            tick(1000);
            subscription.unsubscribe();
        }));

        it('should watch for instance changes at the correct destination', () => {
            expect(watchSpy).toHaveBeenCalledOnceWith('/topic/instances/8');
        });

        it('should track the instance messages', () => {
            expect(result).toEqual(sentMessage);
        });
    });

    describe('watchInstanceDeletion', () => {
        let watchSpy: jasmine.Spy<(destinationOrOptions: string | IWatchParams, headers?: any) => Subject<IMessage>>;
        let result: boolean;

        beforeEach(fakeAsync(() => {
            watchSpy = spyOn(stompClient, 'watch').and.returnValue(
                new BehaviorSubject<IMessage>({} as IMessage).asObservable()
            );
            result = false;
            const subscription = service.watchInstanceDeletion({ id: 8 } as TerrariaInstanceEntity).subscribe({
                next: () => (result = true),
            });
            tick(1000);
            subscription.unsubscribe();
        }));

        it('should watch for instance changes at the correct destination', () => {
            expect(watchSpy).toHaveBeenCalledOnceWith('/topic/instances/8/deletion');
        });

        it('should track the instance messages', () => {
            expect(result).toBeTrue();
        });
    });

    describe('watchInstanceEvents', () => {
        let watchSpy: jasmine.Spy<(destinationOrOptions: string | IWatchParams, headers?: any) => Subject<IMessage>>;
        let result: TerrariaInstanceEventMessage;
        const sentMessage: TerrariaInstanceEventMessage = {
            id: 1,
            text: 'text',
            type: 'OUTPUT',
        };

        beforeEach(fakeAsync(() => {
            watchSpy = spyOn(stompClient, 'watch').and.returnValue(
                new BehaviorSubject<IMessage>({ body: JSON.stringify(sentMessage) } as IMessage).asObservable()
            );
            const subscription = service.watchInstanceEvents({ id: 8 } as TerrariaInstanceEntity).subscribe({
                next: (message) => (result = message),
            });
            tick(1000);
            subscription.unsubscribe();
        }));

        it('should watch for instance changes at the correct destination', () => {
            expect(watchSpy).toHaveBeenCalledOnceWith('/topic/instances/8/events');
        });

        it('should track the instance messages', () => {
            expect(result).toEqual(sentMessage);
        });
    });
});
