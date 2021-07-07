import { AfterViewInit, Component, Input, OnInit, ViewChild } from '@angular/core';
import { MatExpansionPanel } from '@angular/material/expansion';
import { ActivatedRoute } from '@angular/router';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-host-info-page',
    templateUrl: './host-info-page.component.html',
    styleUrls: ['./host-info-page.component.sass'],
})
export class HostInfoPageComponent implements OnInit {
    host?: HostEntity;

    constructor(private readonly restApi: RestApiService, private readonly activatedRoute: ActivatedRoute) {}

    ngOnInit(): void {
        this.activatedRoute.paramMap.subscribe(async (paramMap) => {
            const hostIdParam = paramMap.get('hostId');
            if (!hostIdParam) {
                throw new Error('The [hostId] parameter is not set!');
            }
            this.host = await this.restApi.getHost(parseInt(hostIdParam, 10));
        });
    }

    get loading(): boolean {
        return !this.host;
    }

    get loaded(): boolean {
        return !this.loading;
    }

    get hostUuid(): string | undefined {
        return this.host?.uuid;
    }

    get hostName(): string | undefined {
        return this.host?.name;
    }

    get hostIcon(): string | undefined {
        return this.host?.alive ? 'play_circle_filled' : 'stop';
    }

    get hostStatus(): string {
        return this.host?.alive ? 'running' : 'stopped';
    }

    get terrariaInstances(): TerrariaInstanceEntity[] {
        return this.host?.terrariaInstances || [];
    }

    terrariaInstanceCreated(instance: TerrariaInstanceEntity): void {
        if (this.host) {
            this.host.terrariaInstances.push(instance);
        }
    }
}
