import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TerrariaWorldEntity } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { ListItemInfo, MaterialListItemInfo } from 'src/test-util/list-item-info';

import { TerrariaWorldListItemComponent } from './terraria-world-list-item.component';

describe('TerrariaWorldListItemComponent', () => {
    let fixture: ComponentFixture<TerrariaWorldListItemComponent>;
    let component: TerrariaWorldListItemComponent;
    let listItemInfo: ListItemInfo;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule],
            declarations: [TerrariaWorldListItemComponent, EnUsTranslatePipeStub],
        }).compileComponents();

        const world = {
            id: 1,
            lastModified: '2021-02-13T05:51:24Z',
            name: 'Test World',
        } as TerrariaWorldEntity;

        [fixture, component] = await initComponent(
            TerrariaWorldListItemComponent,
            (createdComponent) => (createdComponent.world = world)
        );
        listItemInfo = new MaterialListItemInfo(fixture);
    });

    it('should have two lines', () => {
        expect(listItemInfo.getLines().length).toBe(2);
    });

    it('should have the world name in the first line', () => {
        expect(listItemInfo.getLines()[0]).toBe('Test World');
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
            fixture.detectChanges();
            tick();
        }));

        it('should have no lines', () => {
            expect(listItemInfo.getLines()).toEqual([]);
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
});
