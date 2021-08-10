import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
    HostEntity,
    TerrariaInstanceEntity,
    TerrariaInstanceEventMessage,
    TerrariaInstanceMessage,
} from 'src/generated/backend';
import { MessageService } from './message.service';

@Injectable()
export class MessageServiceStub implements MessageService {
    watchHostInstanceCreation(_host: HostEntity): Observable<TerrariaInstanceEntity> {
        throw new Error('Method not mocked.');
    }

    watchInstanceChanges(_instance: TerrariaInstanceEntity): Observable<TerrariaInstanceMessage> {
        throw new Error('Method not mocked.');
    }

    watchInstanceDeletion(_instance: TerrariaInstanceEntity): Observable<void> {
        throw new Error('Method not mocked.');
    }

    watchInstanceEvents(_instance: TerrariaInstanceEntity): Observable<TerrariaInstanceEventMessage> {
        throw new Error('Method not mocked.');
    }
}
