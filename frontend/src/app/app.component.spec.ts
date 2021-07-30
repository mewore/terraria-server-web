import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateService } from '@ngx-translate/core';
import { TranslateServiceStub } from 'src/stub/translate.service.stub';
import { AppComponent } from './app.component';
import { AppModule } from './app.module';
import { HeaderBarComponent } from './header/header-bar/header-bar.component';
import { HeaderBarStubComponent } from './header/header-bar/header-bar.component.stub';

describe('AppComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            declarations: [AppComponent, HeaderBarStubComponent],
            providers: [{ provide: TranslateService, useClass: TranslateServiceStub }],
        }).compileComponents();
    });

    async function instantiate(): Promise<AppComponent> {
        const fixture = TestBed.createComponent(AppComponent);
        const component = fixture.componentInstance;
        await fixture.whenStable();
        return component;
    }

    it('should create the app', async () => {
        expect(await instantiate()).toBeTruthy();
    });

    it(`should have the title 'Terraria Server Web'`, async () => {
        expect((await instantiate()).title).toEqual('Terraria Server Web');
    });
});
