import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TerrariaWorldEntity } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent, refreshFixture } from 'src/test-util/angular-test-util';
import { ListItemInfo, MaterialListItemInfo } from 'src/test-util/list-item-info';

import { TerrariaWorldListItemComponent } from './terraria-world-list-item.component';

describe('TerrariaWorldListItemComponent', () => {
    let fixture: ComponentFixture<TerrariaWorldListItemComponent>;
    let component: TerrariaWorldListItemComponent;
    let listItemInfo: ListItemInfo;

    let world: TerrariaWorldEntity;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule],
            declarations: [TerrariaWorldListItemComponent, EnUsTranslatePipeStub],
        }).compileComponents();

        world = {
            id: 1,
            lastModified: '2021-02-13T05:51:24Z',
            displayName: 'Test World',
        } as TerrariaWorldEntity;

        [fixture, component] = await initComponent(
            TerrariaWorldListItemComponent,
            (createdComponent) => (createdComponent.world = world)
        );
        listItemInfo = new MaterialListItemInfo(fixture);
    });

    it('should have two lines', () => {
        expect(listItemInfo.lines.length).toBe(2);
    });

    it('should have the world name in the first line', () => {
        expect(listItemInfo.lines[0]).toBe('Test World');
    });

    describe('deleted', () => {
        it('should be false', () => {
            expect(component.missing).toBeFalse();
        });
    });

    describe('lastModifiedString', () => {
        it('should return a non-empty string', () => {
            expect(component.lastModifiedString).toBeTruthy();
        });
    });

    describe('lastModifiedDetailedString', () => {
        it('should return a non-empty string', () => {
            expect(component.lastModifiedDetailedString).toBeTruthy();
        });
    });

    describe('when the world is undefined', () => {
        beforeEach(fakeAsync(() => {
            component.world = undefined;
            refreshFixture(fixture);
        }));

        it('should have no lines', () => {
            expect(listItemInfo.lines).toEqual([]);
        });

        describe('lastModifiedString', () => {
            it('should return undefined', () => {
                expect(component.lastModifiedString).toBeUndefined();
            });
        });

        describe('lastModifiedDetailedString', () => {
            it('should return undefined', () => {
                expect(component.lastModifiedDetailedString).toBeUndefined();
            });
        });
    });

    describe('when the timestamp of the world is undefined', () => {
        beforeEach(fakeAsync(() => {
            world.lastModified = undefined;
            refreshFixture(fixture);
        }));

        it('should show the world as deleted with no timestamp', () => {
            expect(listItemInfo.lines).toEqual(['Test World (MISSING)']);
        });

        describe('deleted', () => {
            it('should be true', () => {
                expect(component.missing).toBeTrue();
            });
        });
    });
});
