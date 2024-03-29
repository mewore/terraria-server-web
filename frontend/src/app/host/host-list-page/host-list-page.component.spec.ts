import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RestApiService } from 'src/app/core/services/rest-api.service';
import { RestApiServiceStub } from 'src/app/core/services/rest-api.service.stub';
import { HostEntity } from 'src/generated/backend';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { HostListItemStubComponent } from '../host-list-item/host-list-item.component.stub';

import { HostListPageComponent } from './host-list-page.component';

describe('HostListPageComponent', () => {
    let fixture: ComponentFixture<HostListPageComponent>;
    let component: HostListPageComponent;

    let restApiService: RestApiService;

    const init = async (): Promise<unknown> => ([fixture, component] = await initComponent(HostListPageComponent));

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MatListModule, MatProgressSpinnerModule, MatDividerModule],
            declarations: [HostListPageComponent, HostListItemStubComponent, EnUsTranslatePipeStub],
            providers: [{ provide: RestApiService, useClass: RestApiServiceStub }],
        }).compileComponents();

        restApiService = TestBed.inject(RestApiService);
        spyOn(restApiService, 'getLocalHostUuid').and.resolveTo('local-uuid');
    });

    describe('when there are hosts', () => {
        let localHost: HostEntity;
        let remoteHost: HostEntity;
        let otherRemoteHost: HostEntity;
        beforeEach(async () => {
            localHost = { id: 1, uuid: 'local-uuid' } as HostEntity;
            remoteHost = { id: 2, uuid: 'remote-uuid' } as HostEntity;
            otherRemoteHost = { id: 3, uuid: 'other-remote-uuid' } as HostEntity;
            spyOn(restApiService, 'getHosts').and.resolveTo([localHost, remoteHost, otherRemoteHost]);
            await init();
        });

        it('should not be loading', () => {
            expect(component.loading).toBeFalse();
        });

        it('should have the correct local host', () => {
            expect(component.localHost).toBe(localHost);
        });

        it('should have the correct other hosts', () => {
            expect(component.otherHosts).toEqual([remoteHost, otherRemoteHost]);
        });
    });

    describe('when there are no hosts', () => {
        beforeEach(async () => {
            spyOn(restApiService, 'getHosts').and.resolveTo([]);
            await init();
        });

        it('should not be loading', () => {
            expect(component.loading).toBeFalse();
        });

        it('should not have a local host', () => {
            expect(component.localHost).toBeUndefined();
        });

        it('should not have other hosts', () => {
            expect(component.otherHosts).toEqual([]);
        });
    });

    describe('when there are only remote hosts', () => {
        beforeEach(async () => {
            spyOn(restApiService, 'getHosts').and.resolveTo([{ id: 2, uuid: 'remote-uuid' } as HostEntity]);
            await init();
        });

        it('should not have a local host', () => {
            expect(component.localHost).toBeUndefined();
        });
    });
});
