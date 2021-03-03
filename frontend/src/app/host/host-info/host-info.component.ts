import { Component, Input, OnInit } from '@angular/core';
import { HostEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-host-info',
    templateUrl: './host-info.component.html',
    styleUrls: ['./host-info.component.sass'],
})
export class HostInfoComponent implements OnInit {
    @Input()
    host?: HostEntity;

    constructor() {}

    ngOnInit(): void {}

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
