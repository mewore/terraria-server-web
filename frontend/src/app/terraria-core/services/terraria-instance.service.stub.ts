import { Injectable } from '@angular/core';
import { TerrariaInstanceEntity, TerrariaInstanceState } from 'src/generated/backend';
import { TerrariaInstanceService } from './terraria-instance.service';

@Injectable()
export class TerrariaInstanceServiceStub implements TerrariaInstanceService {
    private readonly RUNNING_STATES = new Set<TerrariaInstanceState>([
        'BOOTING_UP',
        'WORLD_MENU',
        'MOD_MENU',
        'CHANGING_MOD_STATE',
        'MOD_BROWSER',
        'WORLD_SIZE_PROMPT',
        'WORLD_DIFFICULTY_PROMPT',
        'WORLD_NAME_PROMPT',
        'MAX_PLAYERS_PROMPT',
        'PORT_PROMPT',
        'AUTOMATICALLY_FORWARD_PORT_PROMPT',
        'PASSWORD_PROMPT',
        'RUNNING',
    ]);

    constructor() {}

    isActive(instance: TerrariaInstanceEntity): boolean {
        return this.RUNNING_STATES.has(instance.state);
    }

    canDelete(instance: TerrariaInstanceEntity | undefined): boolean {
        return !!instance && !this.isActive(instance);
    }

    delete(_instance: TerrariaInstanceEntity | undefined): Promise<TerrariaInstanceEntity | undefined> {
        throw new Error('Method not mocked.');
    }

    getStatusLabel(_instance: TerrariaInstanceEntity | undefined, _deleted: boolean): string {
        throw new Error('Method not mocked.');
    }

    isStateBad(_instance: TerrariaInstanceEntity | undefined, _deleted: boolean): boolean {
        throw new Error('Method not mocked.');
    }
}
