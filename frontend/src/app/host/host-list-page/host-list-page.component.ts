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
    private allHosts?: HostEntity[];

    constructor(private readonly restApi: RestApiService) {}

    get otherHosts(): HostEntity[] | undefined {
        if (!this.localHost || !this.allHosts) {
            return undefined;
        }
        return this.allHosts.filter((host) => host.id !== this.localHost?.id);
    }

    ngOnInit(): void {
        this.restApi.getLocalHost().then((localHost) => (this.localHost = localHost));
        this.restApi.getHosts().then((allHosts) => (this.allHosts = allHosts));
    }
}
