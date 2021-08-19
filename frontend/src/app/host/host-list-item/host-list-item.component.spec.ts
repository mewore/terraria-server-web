import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateService } from '@ngx-translate/core';
import { HostEntity } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { EnUsTranslateServiceStub } from 'src/stubs/translate.service.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { ListItemInfo, MaterialListItemInfo } from 'src/test-util/list-item-info';

import { HostListItemComponent } from './host-list-item.component';

describe('HostListItemComponent', () => {
    let fixture: ComponentFixture<HostListItemComponent>;
    let component: HostListItemComponent;
    let listItemInfo: ListItemInfo;

    let host: HostEntity;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule, RouterTestingModule],
            declarations: [HostListItemComponent, EnUsTranslatePipeStub],
            providers: [{ provide: TranslateService, useClass: EnUsTranslateServiceStub }],
        }).compileComponents();

        host = {
            id: 1,
            alive: true,
            os: 'LINUX',
            terrariaInstanceDirectory: 'dir',
            uuid: 'uuid',
            name: null,
            url: null,
        };

        [fixture, component] = await initComponent(
            HostListItemComponent,
            (createdComponent) => (createdComponent.host = host)
        );
        listItemInfo = new MaterialListItemInfo(fixture);
    });

    it('should have an "online" icon tooltip', () => {
        expect(component.iconTooltip).toBe('Online');
    });

    it('should have the correct lines', () => {
        expect(listItemInfo.lines).toEqual(['[No name]', 'uuid']);
    });

    describe('when the host has a name', () => {
        beforeEach(() => {
            host.name = 'name';
            fixture.detectChanges();
        });

        it('should have the correct lines', () => {
            expect(listItemInfo.lines).toEqual(['name', 'uuid']);
        });
    });

    describe('when the host is dead', () => {
        beforeEach(() => {
            host.alive = false;
        });

        it('should have an "offline" icon tooltip', () => {
            expect(component.iconTooltip).toBe('Offline');
        });
    });

    describe('when there is no host', () => {
        beforeEach(() => {
            component.host = undefined;
            fixture.detectChanges();
        });

        it('should have a "loading" icon tooltip', () => {
            expect(component.iconTooltip).toBe('Loading...');
        });

        it('should have the correct lines', () => {
            expect(listItemInfo.lines).toEqual(['[Loading]', '']);
        });
    });
});
