import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { EnUsTranslatePipeStub } from 'src/stubs/translate.pipe.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { SessionInfoStubComponent } from '../session-info/session-info.component.stub';

import { HeaderBarComponent } from './header-bar.component';

describe('HeaderBarComponent', () => {
    let fixture: ComponentFixture<HeaderBarComponent>;
    let component: HeaderBarComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            declarations: [HeaderBarComponent, EnUsTranslatePipeStub, SessionInfoStubComponent],
        }).compileComponents();

        [fixture, component] = await initComponent(HeaderBarComponent);
    });

    it('should contain the app title', () => {
        const titleElement = (fixture.nativeElement as HTMLElement).querySelector<HTMLSpanElement>('.header-left span');
        expect(titleElement?.innerHTML).toBe('Terraria Server');
    });
});
