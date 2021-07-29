import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TerrariaInstancePageComponent } from './terraria-instance-page/terraria-instance-page.component';

const routes: Routes = [{ path: 'hosts/:hostId/instances/:instanceId', component: TerrariaInstancePageComponent }];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class TerrariaRoutingModule {}
