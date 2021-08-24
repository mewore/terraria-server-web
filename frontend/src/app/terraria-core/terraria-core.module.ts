import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { CoreModule } from '../core/core.module';
import { TerrariaInstanceService, TerrariaInstanceServiceImpl } from './services/terraria-instance.service';
import { TerrariaWorldService, TerrariaWorldServiceImpl } from './services/terraria-world.service';

@NgModule({
    declarations: [],
    imports: [CommonModule, CoreModule],
    providers: [
        { provide: TerrariaInstanceService, useClass: TerrariaInstanceServiceImpl },
        { provide: TerrariaWorldService, useClass: TerrariaWorldServiceImpl },
    ],
})
export class TerrariaCoreModule {}
