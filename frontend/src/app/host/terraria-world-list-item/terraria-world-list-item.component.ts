import { Component, Input, OnInit } from '@angular/core';
import { TerrariaWorldEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-world-list-item',
    templateUrl: './terraria-world-list-item.component.html',
    styleUrls: ['./terraria-world-list-item.component.sass'],
})
export class TerrariaWorldListItemComponent implements OnInit {
    @Input()
    world?: TerrariaWorldEntity;

    constructor() {}

    ngOnInit(): void {}

    get lastModifiedString(): string | undefined {
        return this.world ? new Date(this.world.lastModified).toLocaleString() : undefined;
    }

    get lastModifiedDetailedString(): string | undefined {
        return this.world ? new Date(this.world.lastModified).toString() : undefined;
    }
}
