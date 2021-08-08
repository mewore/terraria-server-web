import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { EnUsTranslateServiceStub } from 'src/stubs/translate.service.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { ListItemInfo, MaterialListItemInfo } from 'src/test-util/list-item-info';

import { NewItemButtonComponent } from './new-item-button.component';

describe('NewItemButtonComponent', () => {
    let fixture: ComponentFixture<NewItemButtonComponent>;
    let component: NewItemButtonComponent;
    let listItemInfo: ListItemInfo;

    let authenticationService: AuthenticationService;

    let pressSubscription: Subscription;
    let pressCount: number;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule],
            declarations: [NewItemButtonComponent, EnUsTranslatePipeStub],
            providers: [
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: TranslateService, useClass: EnUsTranslateServiceStub },
            ],
        }).compileComponents();

        authenticationService = TestBed.inject(AuthenticationService);

        [fixture, component] = await initComponent(NewItemButtonComponent);
        listItemInfo = new MaterialListItemInfo(fixture);

        pressCount = 0;
        pressSubscription = component.press.subscribe(() => pressCount++);
    });

    afterEach(() => {
        pressSubscription.unsubscribe();
    });

    describe('when there are no required permissions', () => {
        it('should not be disabled', () => {
            expect(listItemInfo.disabled).toBeFalse();
        });

        it('should have an undefined disabled reason', () => {
            expect(component.createDisabledReason).toBeUndefined();
        });

        describe('when the item is clicked', () => {
            beforeEach(() =>  listItemInfo.click());

            it('should emit', () => {
                expect(pressCount).toBe(1);
            });
        });
    });

    describe('when there are required permissions', () => {
        beforeEach(async () => {
            component.requiredPermissions = ['ableToManageAccounts', 'ableToManageHosts'];
            fixture.detectChanges();
            await fixture.whenStable();
        });

        describe('when there is no account', () => {
            it('should be disabled', () => {
                expect(listItemInfo.disabled).toBeTrue();
            });

            it('should have a disabled reason for both required permissions', () => {
                expect(component.createDisabledReason).toBe(
                    [
                        'You do not have the permission to manage accounts.',
                        'You do not have the permission to manage hosts.',
                    ].join('\n')
                );
            });

            describe('when the item is clicked', () => {
                beforeEach(() =>  listItemInfo.click());

                it('should not emit', () => {
                    expect(pressCount).toBe(0);
                });
            });
        });

        describe('when there is an account with partial permissions', () => {
            beforeEach(async () => {
                spyOnProperty(authenticationService, 'currentUser', 'get').and.returnValue({
                    accountType: {
                        ableToManageAccounts: true,
                        ableToManageHosts: false,
                        ableToManageTerraria: true,
                    },
                } as AuthenticatedUser);
                fixture.detectChanges();
                await fixture.whenStable();
            });

            it('should be disabled', () => {
                expect(listItemInfo.disabled).toBeTrue();
            });

            it('should have a disabled reason only for the missing permission', () => {
                expect(component.createDisabledReason).toBe('You do not have the permission to manage hosts.');
            });

            describe('when the item is clicked', () => {
                beforeEach(() =>  listItemInfo.click());

                it('should not emit', () => {
                    expect(pressCount).toBe(0);
                });
            });
        });

        describe('when there is an account with full permissions', () => {
            beforeEach(async () => {
                spyOnProperty(authenticationService, 'currentUser', 'get').and.returnValue({
                    accountType: {
                        ableToManageAccounts: true,
                        ableToManageHosts: true,
                        ableToManageTerraria: false,
                    },
                } as AuthenticatedUser);
                fixture.detectChanges();
                await fixture.whenStable();
            });

            it('should not be disabled', () => {
                expect(listItemInfo.disabled).toBeFalse();
            });

            it('should have an undefined disabled reason', () => {
                expect(component.createDisabledReason).toBeUndefined();
            });

            describe('when the item is clicked', () => {
                beforeEach(() =>  listItemInfo.click());

                it('should emit', () => {
                    expect(pressCount).toBe(1);
                });
            });
        });
    });
});
