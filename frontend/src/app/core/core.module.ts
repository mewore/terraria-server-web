import { CommonModule } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { SessionInterceptor } from './incerceptors/session.interceptor';
import { AuthenticationStateService, AuthenticationStateServiceImpl } from './services/authentication-state.service';
import { AuthenticationService, AuthenticationServiceImpl } from './services/authentication.service';
import { ErrorService, ErrorServiceImpl } from './services/error.service';
import { StorageService, StorageServiceImpl } from './services/storage.service';
import { SimpleDialogComponent } from './simple-dialog/simple-dialog.component';
import { SimpleDialogService, SimpleDialogServiceImpl } from './simple-dialog/simple-dialog.service';

@NgModule({
    declarations: [SimpleDialogComponent],
    imports: [
        HttpClientModule,
        CommonModule,
        MatButtonModule,
        MatDialogModule,
        MatProgressBarModule,
        TranslateModule.forChild(),
    ],
    providers: [
        { provide: HTTP_INTERCEPTORS, useClass: SessionInterceptor, multi: true },
        { provide: AuthenticationService, useClass: AuthenticationServiceImpl },
        { provide: AuthenticationStateService, useClass: AuthenticationStateServiceImpl },
        { provide: ErrorService, useClass: ErrorServiceImpl },
        { provide: StorageService, useClass: StorageServiceImpl },
        { provide: SimpleDialogService, useClass: SimpleDialogServiceImpl },
    ],
})
export class CoreModule {}
