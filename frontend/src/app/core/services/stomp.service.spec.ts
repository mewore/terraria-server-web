import { TestBed } from '@angular/core/testing';
import { RxStomp } from '@stomp/rx-stomp';

import { StompService, StompServiceImpl } from './stomp.service';

describe('RxStompService', () => {
    let service: StompService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(StompServiceImpl);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('createStompClient', () => {
        it('should instantaite a STOMP client', () => {
            expect(service.createStompClient() instanceof RxStomp).toBeTrue();
        });
    });
});
