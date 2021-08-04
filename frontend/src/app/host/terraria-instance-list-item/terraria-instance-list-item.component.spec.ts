import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterTestingModule } from '@angular/router/testing';
import { HostEntity, TerrariaInstanceEntity } from 'src/generated/backend';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';

import { TerrariaInstanceListItemComponent } from './terraria-instance-list-item.component';

describe('TerrariaInstanceListItemComponent', () => {
    let fixture: ComponentFixture<TerrariaInstanceListItemComponent>;
    let component: TerrariaInstanceListItemComponent;

    let host: HostEntity;
    let instance: TerrariaInstanceEntity;

    async function instantiate(): Promise<TerrariaInstanceListItemComponent> {
        fixture = TestBed.createComponent(TerrariaInstanceListItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
        return component;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule, RouterTestingModule],
            declarations: [TerrariaInstanceListItemComponent, TranslatePipeStub],
        }).compileComponents();
        await instantiate();

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

    function getLines(): string[] {
        const lineElements = (fixture.nativeElement as HTMLElement).getElementsByClassName('mat-line');
        const lines: string[] = [];
        for (let i = 0; i < lineElements.length; i++) {
            lines.push(lineElements.item(i)?.textContent?.trim().replace(/\s+/g, ' ') || '');
        }
        return lines;
    }

    function getLinkAtLine(lineIndex: number, linkLabel: string): string | undefined {
        const lineElement = (fixture.nativeElement as HTMLElement).getElementsByClassName('mat-line').item(lineIndex);
        if (!lineElement) {
            throw new Error(`There is no line with index ${lineIndex}`);
        }
        const links = lineElement.getElementsByTagName('a');
        for (let i = 0; i < links.length; i++) {
            if (links.item(i)?.textContent?.trim() === linkLabel) {
                return links.item(i)?.getAttribute('href') ?? undefined;
            }
        }
        throw new Error(`There is no link with label '${linkLabel}' at line ${lineIndex}`);
    }

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have the correct lines', () => {
        expect(getLines()).toEqual([
            'Test Instance',
            'TModLoader: 1.0.0 (Download)',
            'Terraria server: 1.2.3 (Download)',
        ]);
    });

    describe('the tModLoader version link', () => {
        it('should point to the correct URL', () => {
            expect(getLinkAtLine(1, '1.0.0')).toBe('mod-loader-release-url');
        });
    });

    describe('the tModLoader download link', () => {
        it('should point to the correct URL', () => {
            expect(getLinkAtLine(1, '(Download)')).toBe('mod-loader-archive-url');
        });
    });

    describe('the server version link', () => {
        it('should point to the correct URL', () => {
            expect(getLinkAtLine(2, '1.2.3')).toBe('https://terraria.gamepedia.com/Server#Downloads');
        });
    });

    describe('the server download link', () => {
        it('should point to the correct URL', () => {
            expect(getLinkAtLine(2, '(Download)')).toBe('server-url');
        });
    });
});
