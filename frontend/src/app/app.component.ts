import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tsw-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.sass'],
})
export class AppComponent {
    title = 'Terraria Server Web';

    constructor(public translateService: TranslateService) {
        const defaultLanguage = 'en-US';
        const languages = [defaultLanguage];
        translateService.addLangs(languages);
        translateService.setDefaultLang(defaultLanguage);

        const browserLanguage = translateService.getBrowserLang();
        const normalizedBrowserLanguage = browserLanguage.split('-')[0];
        const suitableLanguage = languages.find((language) => language.split('-')[0] === normalizedBrowserLanguage);
        translateService.use(suitableLanguage || defaultLanguage);
    }
}
