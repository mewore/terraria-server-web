import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateService } from '@ngx-translate/core';
import { EnUsTranslateServiceStub } from 'src/stubs/translate.service.stub';
import { initComponent } from 'src/test-util/angular-test-util';
import { AppComponent } from './app.component';
import { HeaderBarStubComponent } from './header/header-bar/header-bar.component.stub';

describe('AppComponent', () => {
    let translateService: TranslateService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            declarations: [AppComponent, HeaderBarStubComponent],
            providers: [{ provide: TranslateService, useClass: EnUsTranslateServiceStub }],
        }).compileComponents();

        translateService = TestBed.inject(TranslateService);
    });

    const instantiateAppComponent = async (): Promise<AppComponent> => (await initComponent(AppComponent))[1];

    it('should create the app', async () => {
        expect(await instantiateAppComponent()).toBeTruthy();
    });

    it('should add the en-US language to the list of languages', async () => {
        const addLangsSpy = spyOn(translateService, 'addLangs');

        await instantiateAppComponent();
        expect(addLangsSpy).toHaveBeenCalledWith(['en-US']);
    });

    it('should set the default language to "en-US"', async () => {
        const setDefaultLangSpy = spyOn(translateService, 'setDefaultLang');

        await instantiateAppComponent();
        expect(setDefaultLangSpy).toHaveBeenCalledWith('en-US');
    });

    it('should have the title "Terraria Server Web"', async () => {
        expect((await instantiateAppComponent()).title).toEqual('Terraria Server Web');
    });

    describe('when the browser language is en-GB', () => {
        beforeEach(() => {
            spyOn(translateService, 'getBrowserLang').and.returnValue('en-GB');
        });

        it('should use the en-US language for i18n', async () => {
            const useSpy = spyOn(translateService, 'use');

            await instantiateAppComponent();
            expect(useSpy).toHaveBeenCalledWith('en-US');
        });
    });

    describe('when the browser language is fr-FR', () => {
        beforeEach(() => {
            spyOn(translateService, 'getBrowserLang').and.returnValue('fr-FR');
        });

        it('should use the default (en-US) language for i18n', async () => {
            const useSpy = spyOn(translateService, 'use');

            await instantiateAppComponent();
            expect(useSpy).toHaveBeenCalledWith('en-US');
        });
    });
});
