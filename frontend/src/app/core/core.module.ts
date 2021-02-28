import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { SessionInterceptor } from './incerceptors/session.interceptor';

@NgModule({
    declarations: [],
    imports: [HttpClientModule],
    providers: [{ provide: HTTP_INTERCEPTORS, useClass: SessionInterceptor, multi: true }],
})
export class CoreModule {}
