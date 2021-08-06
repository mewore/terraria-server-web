import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterTestingModule } from '@angular/router/testing';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { ListItemInfo, MaterialListItemInfo } from 'src/test-util/list-item-info';

import { TerrariaInstanceListItemComponent } from './terraria-instance-list-item.component';

describe('TerrariaInstanceListItemComponent', () => {
    let fixture: ComponentFixture<TerrariaInstanceListItemComponent>;
    let component: TerrariaInstanceListItemComponent;
    let listItemInfo: ListItemInfo;

    let host: HostEntity;
    let instance: TerrariaInstanceEntity;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule, RouterTestingModule],
            declarations: [TerrariaInstanceListItemComponent, EnUsTranslatePipeStub],
        }).compileComponents();

        [fixture, component] = await initComponent(TerrariaInstanceListItemComponent);
        listItemInfo = new MaterialListItemInfo(fixture);

        host = {
            id: 1,
        } as HostEntity;
        component.host = host;

        instance = {
            id: 2,
            name: 'Test Instance',
            modLoaderVersion: '1.0.0',
            modLoaderReleaseUrl: 'mod-loader-release-url',
            modLoaderArchiveUrl: 'mod-loader-archive-url',
            terrariaVersion: '1.2.3',
            terrariaServerUrl: 'server-url',
        } as TerrariaInstanceEntity;
        component.instance = instance;

        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('should have the correct lines', () => {
        expect(listItemInfo.getLines()).toEqual([
            'Test Instance',
            'TModLoader: 1.0.0 (Download)',
            'Terraria server: 1.2.3 (Download)',
        ]);
    });

    describe('the tModLoader version link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(1, '1.0.0')).toBe('mod-loader-release-url');
        });
    });

    describe('the tModLoader download link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(1, '(Download)')).toBe('mod-loader-archive-url');
        });
    });

    describe('the server version link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(2, '1.2.3')).toBe('https://terraria.gamepedia.com/Server#Downloads');
        });
    });

    describe('the server download link', () => {
        it('should point to the correct URL', () => {
            expect(listItemInfo.getLinkAtLine(2, '(Download)')).toBe('server-url');
        });
    });
});
