import { Component, Input } from '@angular/core';
import { TerrariaWorldEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-world-list-item',
    templateUrl: './terraria-world-list-item.component.html',
    styleUrls: ['./terraria-world-list-item.component.sass'],
})
export class TerrariaWorldListItemComponent {
    @Input()
    world?: TerrariaWorldEntity;

    get missing(): boolean {
        return !!this.world && this.world.lastModified == null;
    }

    get lastModifiedString(): string | undefined {
        return this.world?.lastModified ? new Date(this.world.lastModified).toLocaleString() : undefined;
    }

    get lastModifiedDetailedString(): string | undefined {
        return this.world?.lastModified ? new Date(this.world.lastModified).toString() : undefined;
    }
}
