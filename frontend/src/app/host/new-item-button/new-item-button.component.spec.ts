import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { AuthenticationServiceStub } from 'src/app/core/services/authentication.service.stub';
import { AuthenticatedUser } from 'src/app/core/types';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { TranslateServiceStub } from 'src/stubs/translate.service.stub';
import { HostListItemStubComponent } from '../host-list-item/host-list-item.component.stub';

import { NewItemButtonComponent } from './new-item-button.component';

describe('NewItemButtonComponent', () => {
    let fixture: ComponentFixture<NewItemButtonComponent>;
    let component: NewItemButtonComponent;

    let authenticationService: AuthenticationService;

    let pressSubscription: Subscription;
    let pressCount: number;

    async function instantiate(): Promise<NewItemButtonComponent> {
        fixture = TestBed.createComponent(NewItemButtonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
        return component;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatIconModule, MatTooltipModule],
            declarations: [NewItemButtonComponent, HostListItemStubComponent, TranslatePipeStub],
            providers: [
                { provide: AuthenticationService, useClass: AuthenticationServiceStub },
                { provide: TranslateService, useClass: TranslateServiceStub },
            ],
        }).compileComponents();

        authenticationService = TestBed.inject(AuthenticationService);
        await instantiate();
        pressCount = 0;
        pressSubscription = component.press.subscribe(() => pressCount++);
    });

    afterEach(() => {
        pressSubscription.unsubscribe();
    });

    function getItemElement(): HTMLElement | null {
        return (fixture.nativeElement as HTMLElement)
            .getElementsByClassName('mat-list-item')
            .item(0) as HTMLElement | null;
    }

    async function clickEmits(): Promise<boolean> {
        const itemElement = getItemElement();
        if (!itemElement) {
            throw new Error('The item element does not exist');
        }
        const oldPressCount = pressCount;
        itemElement.click();
        fixture.detectChanges();
        await fixture.whenStable();
        return pressCount > oldPressCount;
    }

    function isDisabled(): boolean | undefined {
        return getItemElement()?.classList.contains('mat-list-item-disabled');
    }

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('when there are no required permissions', () => {
        it('should not be disabled', () => {
            expect(isDisabled()).toBeFalse();
        });

        it('should have an undefined disabled reason', () => {
            expect(component.createDisabledReason).toBeUndefined();
        });

        describe('when the item is clicked', () => {
            it('should emit', async () => {
                expect(await clickEmits()).toBeTrue();
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
                expect(isDisabled()).toBeTrue();
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
                it('should not emit', async () => {
                    expect(await clickEmits()).toBeFalse();
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
                expect(isDisabled()).toBeTrue();
            });

            it('should have a disabled reason only for the missing permission', () => {
                expect(component.createDisabledReason).toBe('You do not have the permission to manage hosts.');
            });

            describe('when the item is clicked', () => {
                it('should not emit', async () => {
                    expect(await clickEmits()).toBeFalse();
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
                expect(isDisabled()).toBeFalse();
            });

            it('should have an undefined disabled reason', () => {
                expect(component.createDisabledReason).toBeUndefined();
            });

            describe('when the item is clicked', () => {
                it('should emit', async () => {
                    expect(await clickEmits()).toBeTrue();
                });
            });
        });
    });
});
