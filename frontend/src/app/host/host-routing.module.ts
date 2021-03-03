import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HostListPageComponent } from './host-list-page/host-list-page.component';

const routes: Routes = [
    { path: 'hosts', component: HostListPageComponent },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class HostRoutingModule {}
