import { Component, Input, OnInit } from '@angular/core';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-instance-list-item',
    templateUrl: './terraria-instance-list-item.component.html',
    styleUrls: ['./terraria-instance-list-item.component.sass'],
})
export class TerrariaInstanceListItemComponent {
    @Input()
    host?: HostEntity;

    @Input()
    instance?: TerrariaInstanceEntity;
}
