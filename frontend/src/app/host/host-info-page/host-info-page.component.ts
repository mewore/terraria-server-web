import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { HostEntity, TerrariaInstanceEntity, TerrariaWorldEntity } from 'src/generated/backend';
import { CreateTerrariaInstanceDialogService } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service';

@Component({
    selector: 'tsw-host-info-page',
    templateUrl: './host-info-page.component.html',
    styleUrls: ['./host-info-page.component.sass'],
})
export class HostInfoPageComponent implements OnInit {
    loading = false;

    host?: HostEntity;

    instances?: TerrariaInstanceEntity[];

    worlds?: TerrariaWorldEntity[];

    constructor(
        private readonly restApi: RestApiService,
        private readonly activatedRoute: ActivatedRoute,
        private readonly createDialogService: CreateTerrariaInstanceDialogService
    ) {}

    async ngOnInit(): Promise<void> {
        this.loading = true;
        this.activatedRoute.paramMap.subscribe(async (paramMap) => {
            try {
                const hostIdParam = paramMap.get('hostId');
                if (!hostIdParam) {
                    throw new Error('The [hostId] parameter is not set!');
                }
                const hostId = parseInt(hostIdParam, 10);
                [this.host, this.instances, this.worlds] = await Promise.all([
                    this.restApi.getHost(hostId),
                    this.restApi.getHostInstances(hostId),
                    this.restApi.getHostWorlds(hostId),
                ]);
            } finally {
                this.loading = false;
            }
        });
    }

    get loaded(): boolean {
        return !this.loading;
    }

    async terrariaInstanceCreationRequested(): Promise<void> {
        if (!this.host || !this.instances) {
            throw new Error('The data has not been loaded. Cannot create a Terraria instance.');
        }
        const newInstance = await this.createDialogService.openDialog(this.host);
        if (newInstance) {
            this.instances.push(newInstance);
        }
    }
}
