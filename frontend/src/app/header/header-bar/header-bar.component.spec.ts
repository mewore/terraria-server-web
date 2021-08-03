import { TestBed } from '@angular/core/testing';
import { TranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { SessionInfoStubComponent } from '../session-info/session-info.component.stub';

import { HeaderBarComponent } from './header-bar.component';

describe('HeaderBarComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HeaderBarComponent, TranslatePipeStub, SessionInfoStubComponent],
        }).compileComponents();
    });

    async function instantiate(): Promise<HeaderBarComponent> {
        const fixture = TestBed.createComponent(HeaderBarComponent);
        await fixture.whenStable();
        return fixture.componentInstance;
    }

    it('should create', async () => {
        expect(await instantiate()).toBeTruthy();
    });
});
