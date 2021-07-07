import { AfterViewInit, Component, Input, OnInit, ViewChild } from '@angular/core';
import { MatExpansionPanel } from '@angular/material/expansion';
import { ActivatedRoute } from '@angular/router';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';
import { CreateTerrariaInstanceDialogService } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service';

@Component({
    selector: 'tsw-host-info-page',
    templateUrl: './host-info-page.component.html',
    styleUrls: ['./host-info-page.component.sass'],
})
export class HostInfoPageComponent implements OnInit {
    host?: HostEntity;

    constructor(
        private readonly restApi: RestApiService,
        private readonly activatedRoute: ActivatedRoute,
        private readonly createDialogService: CreateTerrariaInstanceDialogService
    ) {}

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

    async terrariaInstanceCreationRequested(): Promise<void> {
        if (!this.host) {
            throw new Error('The host is not defined. Cannot create a Terraria instance.');
        }
        const newInstance = await this.createDialogService.openDialog(this.host);
        if (newInstance) {
            this.host.instances.push(newInstance);
        }
    }
}
