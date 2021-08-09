import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { CoreModule } from '../core/core.module';
import { TerrariaInstanceService, TerrariaInstanceServiceImpl } from './services/terraria-instance.service';

@NgModule({
    declarations: [],
    imports: [CommonModule, CoreModule],
    providers: [{ provide: TerrariaInstanceService, useClass: TerrariaInstanceServiceImpl }],
})
export class TerrariaCoreModule {}
