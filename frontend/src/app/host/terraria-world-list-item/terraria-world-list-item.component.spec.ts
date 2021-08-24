import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Observable } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { MessageService } from 'src/app/core/services/message.service';
import { MessageServiceStub } from 'src/app/core/services/message.service.stub';
import { TerrariaWorldService } from 'src/app/terraria-core/services/terraria-world.service';
import { TerrariaWorldServiceStub } from 'src/app/terraria-core/services/terraria-world.service.stub';
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

    let authenticationService: AuthenticationService;

    let terrariaWorldService: TerrariaWorldService;

    let messageService: MessageService;

    let worldDeletionSubject: Subject<void>;
    let watchWorldDeletionSpy: jasmine.Spy<(world: TerrariaWorldEntity) => Observable<void>>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule],
            declarations: [TerrariaWorldListItemComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: TerrariaWorldService, useClass: TerrariaWorldServiceStub },
                { provide: MessageService, useClass: MessageServiceStub },
            ],
        }).compileComponents();

        authenticationService = TestBed.inject(AuthenticationService);
        messageService = TestBed.inject(MessageService);
        terrariaWorldService = TestBed.inject(TerrariaWorldService);

        world = {
            id: 1,
            lastModified: '2021-02-13T05:51:24Z',
            displayName: 'Test World',
        } as TerrariaWorldEntity;

        [fixture, component] = await initComponent(TerrariaWorldListItemComponent);
        listItemInfo = new MaterialListItemInfo(fixture);

        worldDeletionSubject = new Subject();
        watchWorldDeletionSpy = spyOn(messageService, 'watchWorldDeletion').and.returnValue(
            worldDeletionSubject.asObservable()
        );

        component.world = world;
        fakeAsync(() => refreshFixture(fixture))();
    });

    it('should have two lines', () => {
        expect(listItemInfo.lines.length).toBe(2);
    });

    it('should have the world name in the first line', () => {
        expect(listItemInfo.lines[0]).toBe('Test World');
    });

    describe('deleted', () => {
        it('should be false', () => {
            expect(component.deleted).toBeFalse();
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

        describe('the Rename button', () => {
            it('should be disabled', () => {
                expect(listItemInfo.getButton('delete')?.disabled).toBeTrue();
            });
        });

        describe('when terrariaWorldService#canDelete returns true', () => {
            beforeEach(fakeAsync(() => {
                spyOn(terrariaWorldService, 'canDelete').and.returnValue(true);
                refreshFixture(fixture);
            }));

            describe('canDelete', () => {
                it('should be true', () => {
                    expect(component.canDelete).toBeTrue();
                });
            });
        });

        describe('onDeleteClicked', () => {
            let deleteWorldSpy: jasmine.Spy;

            beforeEach(() => {
                deleteWorldSpy = spyOn(terrariaWorldService, 'delete');
                component.onDeleteClicked();
            });

            it('should do nothing', () => {
                expect(deleteWorldSpy).not.toHaveBeenCalled();
            });
        });
    });

    describe('when the timestamp of the world is null', () => {
        beforeEach(fakeAsync(() => {
            world.lastModified = null;
            refreshFixture(fixture);
        }));

        it('should show the world as deleted with no timestamp', () => {
            expect(listItemInfo.lines).toEqual(['Test World (MISSING)']);
        });

        describe('canDownload', () => {
            it('should be false', () => {
                expect(component.canDownload).toBeFalse();
            });
        });

        describe('missing', () => {
            it('should be true', () => {
                expect(component.missing).toBeTrue();
            });
        });
    });

    describe('when the world is deleted', () => {
        beforeEach(fakeAsync(() => {
            worldDeletionSubject.next();
            refreshFixture(fixture);
        }));

        describe('deleted', () => {
            it('should be true', () => {
                expect(component.deleted).toBeTrue();
            });
        });

        describe('canDownload', () => {
            it('should be false', () => {
                expect(component.canDownload).toBeFalse();
            });
        });

        describe('the world name line', () => {
            it('should end with "(DELETED)"', () => {
                expect(listItemInfo.lines[0]).toMatch(/ \(DELETED\)$/);
            });
        });

        describe('when the world is set to another one with the same ID', () => {
            beforeEach(fakeAsync(() => {
                component.world = { id: world.id } as TerrariaWorldEntity;
                refreshFixture(fixture);
            }));

            describe('deleted', () => {
                it('should still be true', () => {
                    expect(component.deleted).toBeTrue();
                });
            });
        });

        describe('when the world is set to another one with a different ID', () => {
            beforeEach(fakeAsync(() => {
                component.world = { id: world.id + 1 } as TerrariaWorldEntity;
                refreshFixture(fixture);
            }));

            describe('deleted', () => {
                it('should be false', () => {
                    expect(component.deleted).toBeFalse();
                });
            });
        });
    });

    describe('when not allowed to manage hosts', () => {
        beforeEach(fakeAsync(() => {
            spyOnProperty(authenticationService, 'canManageHosts', 'get').and.returnValue(false);
            refreshFixture(fixture);
        }));

        describe('the Delete button', () => {
            it('should be disabled', () => {
                expect(listItemInfo.getButton('delete')?.disabled).toBeTrue();
            });
        });

        describe('onDeleteClicked', () => {
            let deleteWorldSpy: jasmine.Spy;

            beforeEach(() => {
                deleteWorldSpy = spyOn(terrariaWorldService, 'delete');
                component.onDeleteClicked();
            });

            it('should do nothing', () => {
                expect(deleteWorldSpy).not.toHaveBeenCalled();
            });
        });
    });

    describe('when the world is used', () => {
        beforeEach(fakeAsync(() => {
            component.usedWorldIds = new Set<number>([world.id]);
            refreshFixture(fixture);
        }));

        describe('the Delete button', () => {
            it('should be disabled', () => {
                expect(listItemInfo.getButton('delete')?.disabled).toBeTrue();
            });
        });
    });

    describe('when the Delete button is clicked', () => {
        let deleteSpy: jasmine.Spy<(world?: TerrariaWorldEntity) => Promise<boolean | undefined>>;
        let oldWorld: TerrariaWorldEntity | undefined;

        describe('when the deletion returns undefined', () => {
            beforeEach(() => {
                oldWorld = component.world;
                deleteSpy = spyOn(terrariaWorldService, 'delete').and.resolveTo(undefined);
                listItemInfo.clickButton('delete');
            });

            it('should keep the world', () => {
                expect(component.world).toBe(oldWorld);
            });

            it('should not mark the world as deleted', () => {
                expect(component.deleted).toBeFalse();
            });
        });

        describe('when the deletion is confirmed', () => {
            beforeEach(() => {
                oldWorld = component.world;
                deleteSpy = spyOn(terrariaWorldService, 'delete').and.resolveTo(true);
                listItemInfo.clickButton('delete');
            });

            it('should request to delete the world', () => {
                expect(deleteSpy).toHaveBeenCalledOnceWith(oldWorld);
            });

            it('should mark the world as deleted', () => {
                expect(component.deleted).toBeTrue();
            });
        });
    });
});
