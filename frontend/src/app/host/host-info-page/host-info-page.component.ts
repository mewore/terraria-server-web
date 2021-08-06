import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { ErrorService } from 'src/app/core/services/error.service';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { HostEntity, TerrariaInstanceEntity, TerrariaWorldEntity } from 'src/generated/backend';
import { CreateTerrariaInstanceDialogService } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service';

@Component({
    selector: 'tsw-host-info-page',
    templateUrl: './host-info-page.component.html',
    styleUrls: ['./host-info-page.component.sass'],
})
export class HostInfoPageComponent implements OnInit, OnDestroy {
    loading = false;

    host?: HostEntity;

    instances?: TerrariaInstanceEntity[];

    worlds?: TerrariaWorldEntity[];

    routeSubscription?: Subscription;

    constructor(
        private readonly restApi: RestApiService,
        private readonly activatedRoute: ActivatedRoute,
        private readonly createDialogService: CreateTerrariaInstanceDialogService,
        private readonly errorService: ErrorService
    ) {}

    async ngOnInit(): Promise<void> {
        this.routeSubscription = this.activatedRoute.paramMap.subscribe(async (paramMap) => {
            this.loading = true;
            try {
                const hostIdParam = paramMap.get('hostId');
                if (!hostIdParam) {
                    this.errorService.showError(new Error('The [hostId] parameter is not set!'));
                    return;
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

    ngOnDestroy(): void {
        this.routeSubscription?.unsubscribe();
    }

    get loaded(): boolean {
        return !this.loading;
    }

    async terrariaInstanceCreationRequested(): Promise<void> {
        if (!this.host || !this.instances) {
            this.errorService.showError(new Error('The data has not been loaded. Cannot create a Terraria instance.'));
            return;
        }
        const newInstance = await this.createDialogService.openDialog(this.host);
        if (newInstance) {
            this.instances.push(newInstance);
        }
    }
}
