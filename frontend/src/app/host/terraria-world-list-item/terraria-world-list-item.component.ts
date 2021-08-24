import { Component, Input } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { MessageService } from 'src/app/core/services/message.service';
import { TerrariaWorldService } from 'src/app/terraria-core/services/terraria-world.service';
import { TerrariaWorldEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-terraria-world-list-item',
    templateUrl: './terraria-world-list-item.component.html',
    styleUrls: ['./terraria-world-list-item.component.sass'],
})
export class TerrariaWorldListItemComponent {
    @Input()
    set world(newWorld: TerrariaWorldEntity | undefined) {
        this.privateWorld = newWorld;
        if (newWorld) {
            this.clearWorldSubscriptions();
            const worldId = newWorld.id;
            this.worldSubscriptions.push(
                this.messageService.watchWorldDeletion(newWorld).subscribe({
                    next: () => this.deleteWorldIds.add(worldId),
                })
            );
        }
    }

    get world(): TerrariaWorldEntity | undefined {
        return this.privateWorld;
    }

    @Input()
    usedWorldIds = new Set<number>();

    private privateWorld?: TerrariaWorldEntity;

    private readonly deleteWorldIds = new Set<number>();

    private worldSubscriptions: Subscription[] = [];

    constructor(
        private readonly authenticationService: AuthenticationService,
        private readonly messageService: MessageService,
        private readonly terrariaWorldService: TerrariaWorldService
    ) {}

    get canDownload(): boolean {
        return !!this.world && !this.missing && !this.deleted;
    }

    get missing(): boolean {
        return !!this.world && this.world.lastModified == null;
    }

    get lastModifiedString(): string | undefined {
        return this.world?.lastModified ? new Date(this.world.lastModified).toLocaleString() : undefined;
    }

    get lastModifiedDetailedString(): string | undefined {
        return this.world?.lastModified ? new Date(this.world.lastModified).toString() : undefined;
    }

    get canDelete(): boolean {
        return (
            this.terrariaWorldService.canDelete(this.world, this.usedWorldIds) && this.canManageHosts && !this.deleted
        );
    }

    get canManageHosts(): boolean {
        return this.authenticationService.canManageHosts;
    }

    get deleted(): boolean {
        return !!this.world && this.deleteWorldIds.has(this.world.id);
    }

    onDeleteClicked(): void {
        const world = this.world;
        if (world && this.canDelete) {
            this.terrariaWorldService.delete(world).then((result) => {
                if (result) {
                    this.deleteWorldIds.add(world.id);
                }
            });
        }
    }

    private clearWorldSubscriptions(): void {
        this.worldSubscriptions.forEach((subscription) => subscription.unsubscribe());
        this.worldSubscriptions = [];
    }
}
