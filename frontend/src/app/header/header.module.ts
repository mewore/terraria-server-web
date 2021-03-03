import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { AuthenticationModule } from '../authentication/authentication.module';
import { CoreModule } from '../core/core.module';
import { HeaderBarComponent } from './header-bar/header-bar.component';
import { SessionInfoComponent } from './session-info/session-info.component';

@NgModule({
    declarations: [SessionInfoComponent, HeaderBarComponent],
    imports: [
        CommonModule,
        TranslateModule.forChild(),
        MatButtonModule,
        MatIconModule,
        MatTooltipModule,
        CoreModule,
        AuthenticationModule,
    ],
    exports: [HeaderBarComponent],
})
export class HeaderModule {}
