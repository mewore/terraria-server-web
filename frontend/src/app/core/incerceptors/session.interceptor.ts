import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthenticationStateService, SessionState } from '../services/authentication-state.service';

@Injectable()
export class SessionInterceptor implements HttpInterceptor {
    constructor(private authenticationStateService: AuthenticationStateService) {}

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const authData = this.authenticationStateService.authData;
        if (authData && (this.isAuthenticated() || this.requestRequiresAuthentication(request))) {
            request = request.clone({
                headers: request.headers.set('Authorization', authData),
            });
        }

        const result = next.handle(request);
        if (authData) {
            return result.pipe(
                catchError((error) => {
                    if (error instanceof HttpErrorResponse && error.status === 401) {
                        this.authenticationStateService.markAsUnsure();
                    }
                    return throwError(error);
                })
            );
        }
        return result;
    }

    private isAuthenticated(): boolean {
        return this.authenticationStateService.sessionState === SessionState.AUTHENTICATED;
    }

    private requestRequiresAuthentication(request: HttpRequest<any>): boolean {
        return request.method !== 'GET' && request.url !== '/auth/login' && request.url !== '/auth/signup';
    }
}
