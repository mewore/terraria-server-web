import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HostInfoPageComponent } from './host-info-page/host-info-page.component';
import { HostListPageComponent } from './host-list-page/host-list-page.component';

const routes: Routes = [
    { path: 'hosts', component: HostListPageComponent },
    { path: 'hosts/:hostId', component: HostInfoPageComponent },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class HostRoutingModule {}
