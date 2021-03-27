import { Component, Input, OnInit } from '@angular/core';
import { TerrariaWorldEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-world-card',
    templateUrl: './terraria-world-card.component.html',
    styleUrls: ['./terraria-world-card.component.sass'],
})
export class TerrariaWorldCardComponent implements OnInit {
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
