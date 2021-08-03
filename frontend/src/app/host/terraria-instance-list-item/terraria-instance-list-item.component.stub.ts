import { Component, Input } from '@angular/core';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-instance-list-item',
    template: '',
})
export class TerrariaInstanceListItemStubComponent {
    @Input()
    host?: HostEntity;

    @Input()
    instance?: TerrariaInstanceEntity;
}
