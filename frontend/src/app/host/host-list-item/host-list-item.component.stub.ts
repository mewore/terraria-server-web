import { Component, Input } from '@angular/core';
import { HostEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-host-list-item',
    template: '',
})
export class HostListItemStubComponent {
    @Input()
    host?: HostEntity;
}
