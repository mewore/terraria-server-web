import { Component, OnInit } from '@angular/core';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { HostEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-host-list-page',
    templateUrl: './host-list-page.component.html',
    styleUrls: ['./host-list-page.component.sass'],
})
export class HostListPageComponent implements OnInit {
    localHost?: HostEntity;

    readonly otherHosts: HostEntity[] = [];

    loading = true;

    constructor(private readonly restApi: RestApiService) {}

    async ngOnInit(): Promise<void> {
        try {
            const [localHostUuid, allHosts] = await Promise.all([
                this.restApi.getLocalHostUuid(),
                this.restApi.getHosts(),
            ]);
            for (const host of allHosts) {
                if (host.uuid === localHostUuid) {
                    this.localHost = host;
                } else {
                    this.otherHosts.push(host);
                }
            }
        } finally {
            this.loading = false;
        }
    }
}
