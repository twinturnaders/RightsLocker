import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private auth = inject(AuthService);
  private refreshing = false;

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.auth.token;
    const authedReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

    return next.handle(authedReq).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401 && !this.refreshing) {
          this.refreshing = true;
          return this.auth.refresh().pipe(
            switchMap(newToken => {
              this.refreshing = false;
              if (!newToken) return throwError(() => err);
              const retried = req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } });
              return next.handle(retried);
            })
          );
        }
        return throwError(() => err);
      })
    );
  }
}
