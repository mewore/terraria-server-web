import { Component, Input } from '@angular/core';
import { TerrariaWorldEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-world-list-item',
    template: '',
})
export class TerrariaWorldListItemStubComponent {
    @Input()
    world?: TerrariaWorldEntity;

    @Input()
    usedWorldIds?: Set<number>;
}
