import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TerrariaInstanceEntity, TerrariaInstanceEventMessage, TerrariaInstanceMessage } from 'src/generated/backend';
import { MessageService } from './message.service';

@Injectable()
export class MessageServiceStub implements MessageService {
    watchInstanceChanges(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceMessage> {
        throw new Error('Method not mocked.');
    }
    watchInstanceDeletion(instance: TerrariaInstanceEntity): Observable<void> {
        throw new Error('Method not mocked.');
    }
    watchInstanceEvents(instance: TerrariaInstanceEntity): Observable<TerrariaInstanceEventMessage> {
        throw new Error('Method not mocked.');
    }
}
