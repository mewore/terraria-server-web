import { Injectable } from '@angular/core';
import { TerrariaWorldEntity } from 'src/generated/backend';
import { TerrariaWorldService } from './terraria-world.service';

@Injectable()
export class TerrariaWorldServiceStub implements TerrariaWorldService {
    canDelete(world: TerrariaWorldEntity | undefined, usedWorldIds: Set<number>): boolean {
        return !!world && !usedWorldIds.has(world.id);
    }

    delete(_world: TerrariaWorldEntity | undefined): Promise<boolean | undefined> {
        throw new Error('Method not mocked.');
    }
}
