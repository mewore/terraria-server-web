import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateService } from '@ngx-translate/core';
import { HostEntity } from 'src/generated/backend';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { TranslateServiceStub } from 'src/stubs/translate.service.stub';

import { HostListItemComponent } from './host-list-item.component';

describe('HostListItemComponent', () => {
    let component: HostListItemComponent;
    let fixture: ComponentFixture<HostListItemComponent>;
    let host: HostEntity;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule, RouterTestingModule],
            declarations: [HostListItemComponent, TranslatePipeStub],
            providers: [{ provide: TranslateService, useClass: TranslateServiceStub }],
        }).compileComponents();
        fixture = TestBed.createComponent(HostListItemComponent);
        component = fixture.componentInstance;
        host = {
            id: 1,
            alive: true,
            os: 'LINUX',
            terrariaInstanceDirectory: 'dir',
            uuid: 'uuid',
        };
        component.host = host;
        fixture.detectChanges();
        await fixture.whenStable();
    });

    function getLines(): string[] {
        const lineElements = (fixture.nativeElement as HTMLElement).getElementsByClassName('mat-line');
        const lines: string[] = [];
        for (let i = 0; i < lineElements.length; i++) {
            lines.push(lineElements.item(i)?.textContent?.trim() || '');
        }
        return lines;
    }

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have an "online" icon tooltip', () => {
        expect(component.iconTooltip).toBe('Online');
    });

    it('should have the correct lines', () => {
        expect(getLines()).toEqual(['[No name]', 'uuid']);
    });

    describe('when the host has a name', () => {
        beforeEach(() => {
            host.name = 'name';
            fixture.detectChanges();
        });

        it('should have the correct lines', () => {
            expect(getLines()).toEqual(['name', 'uuid']);
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
            expect(getLines()).toEqual(['[Loading]', '']);
        });
    });
});
