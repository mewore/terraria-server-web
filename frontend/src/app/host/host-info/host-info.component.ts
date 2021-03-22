import { AfterViewInit, Component, Input, OnInit, ViewChild } from '@angular/core';
import { MatExpansionPanel } from '@angular/material/expansion';
import { HostEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-host-info',
    templateUrl: './host-info.component.html',
    styleUrls: ['./host-info.component.sass'],
})
export class HostInfoComponent implements AfterViewInit {
    @ViewChild(MatExpansionPanel)
    panel?: MatExpansionPanel;

    @Input()
    host?: HostEntity;

    @Input()
    hostIsLocal?: boolean;

    constructor() {}

    ngAfterViewInit(): void {
        if (this.hostIsLocal && this.panel) {
            // TODO: Open the panel without the "expression changed after it has been checked" error and without
            // `setTimeout`
            setTimeout(() => this.panel?.open());
        }
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
}
