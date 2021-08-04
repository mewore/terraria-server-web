import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TerrariaWorldEntity } from 'src/generated/backend';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';

import { TerrariaWorldListItemComponent } from './terraria-world-list-item.component';

describe('TerrariaWorldListItemComponent', () => {
    let fixture: ComponentFixture<TerrariaWorldListItemComponent>;
    let component: TerrariaWorldListItemComponent;

    let world: TerrariaWorldEntity;

    async function instantiate(): Promise<TerrariaWorldListItemComponent> {
        fixture = TestBed.createComponent(TerrariaWorldListItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
        return component;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule],
            declarations: [TerrariaWorldListItemComponent, TranslatePipeStub],
        }).compileComponents();
        await instantiate();

        world = {
            id: 1,
            lastModified: '2021-02-13T05:51:24Z',
            name: 'Test World',
        } as TerrariaWorldEntity;
        component.world = world;

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

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have two lines', () => {
        expect(getLines().length).toBe(2);
    });

    it('should have the world name in the first line', () => {
        expect(getLines()[0]).toBe('Test World');
    });
});
