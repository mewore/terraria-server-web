import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateService } from '@ngx-translate/core';
import { TranslateServiceStub } from 'src/stubs/translate.service.stub';
import { AppComponent } from './app.component';
import { HeaderBarStubComponent } from './header/header-bar/header-bar.component.stub';

describe('AppComponent', () => {
    let translateService: TranslateServiceStub;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            declarations: [AppComponent, HeaderBarStubComponent],
            providers: [{ provide: TranslateService, useClass: TranslateServiceStub }],
        }).compileComponents();

        translateService = TestBed.inject(TranslateService);
    });

    async function instantiate(): Promise<AppComponent> {
        const fixture = TestBed.createComponent(AppComponent);
        await fixture.whenStable();
        return fixture.componentInstance;
    }

    it('should create the app', async () => {
        expect(await instantiate()).toBeTruthy();
    });

    it(`should add the en-US language to the list of languages`, async () => {
        const addLangsSpy = spyOn(translateService, 'addLangs');

        await instantiate();
        expect(addLangsSpy).toHaveBeenCalledWith(['en-US']);
    });

    it(`should set the default language to 'en-US'`, async () => {
        const setDefaultLangSpy = spyOn(translateService, 'setDefaultLang');

        await instantiate();
        expect(setDefaultLangSpy).toHaveBeenCalledWith('en-US');
    });

    it(`should have the title 'Terraria Server Web'`, async () => {
        expect((await instantiate()).title).toEqual('Terraria Server Web');
    });

    describe('when the browser language is en-GB', () => {
        beforeEach(() => {
            spyOn(translateService, 'getBrowserLang').and.returnValue('en-GB');
        });

        it('should use the en-US language for i18n', async () => {
            const useSpy = spyOn(translateService, 'use');

            await instantiate();
            expect(useSpy).toHaveBeenCalledWith('en-US');
        });
    });

    describe('when the browser language is fr-FR', () => {
        beforeEach(() => {
            spyOn(translateService, 'getBrowserLang').and.returnValue('fr-FR');
        });

        it('should use the default (en-US) language for i18n', async () => {
            const useSpy = spyOn(translateService, 'use');

            await instantiate();
            expect(useSpy).toHaveBeenCalledWith('en-US');
        });
    });
});
