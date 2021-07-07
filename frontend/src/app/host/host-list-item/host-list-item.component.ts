import { Component, Input, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { HostEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-host-list-item',
    templateUrl: './host-list-item.component.html',
    styleUrls: ['./host-list-item.component.sass'],
})
export class HostListItemComponent {
    @Input()
    host?: HostEntity;

    constructor(private readonly translateService: TranslateService) {}

    get descriptionData(): {[key: string]: number | string} {
        return {
            instanceCount: this.host?.instances.length ?? '?',
            worldCount: this.host?.worlds.length ?? '?',
        };
    }

    get iconTooltip(): string {
        if (!this.host) {
            return this.translate('status.loading');
        }
        return this.translate(this.host.alive ? 'status.online' : 'status.offline');
    }

    private translate(subkey: string): string {
        return this.translateService.instant('host-list-item.' + subkey);
    }
}
