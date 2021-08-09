import { Injectable } from '@angular/core';
import { TerrariaInstanceEntity, TerrariaInstanceState } from 'src/generated/backend';
import { TerrariaInstanceService } from './terraria-instance.service';

@Injectable()
export class TerrariaInstanceServiceStub implements TerrariaInstanceService {
    private readonly RUNNING_STATES = new Set<TerrariaInstanceState>([
        'BOOTING_UP',
        'WORLD_MENU',
        'MOD_MENU',
        'MOD_BROWSER',
        'CHANGING_MOD_STATE',
        'MAX_PLAYERS_PROMPT',
        'PORT_PROMPT',
        'AUTOMATICALLY_FORWARD_PORT_PROMPT',
        'PASSWORD_PROMPT',
        'RUNNING',
    ]);

    constructor() {}

    isRunning(instance: TerrariaInstanceEntity): boolean {
        return this.RUNNING_STATES.has(instance.state);
    }

    canDelete(instance: TerrariaInstanceEntity | undefined): boolean {
        return !!instance && !this.isRunning(instance);
    }

    delete(_instance: TerrariaInstanceEntity | undefined): Promise<TerrariaInstanceEntity | undefined> {
        throw new Error('Method not mocked.');
    }
}
