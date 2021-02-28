import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthenticationStateService } from '../services/authentication-state.service';

@Injectable()
export class SessionInterceptor implements HttpInterceptor {
    constructor(private authenticationStateService: AuthenticationStateService) {}

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const authData = this.authenticationStateService.authData;
        if (authData) {
            request = request.clone({
                headers: request.headers.set('Authorization', authData),
            });
        }

        const result = next.handle(request);
        if (authData) {
            result.toPromise().catch((error) => {
                if (error instanceof HttpErrorResponse && error.status === 401) {
                    this.authenticationStateService.markAsUnsure();
                }
            });
        }
        return result;
    }
}
