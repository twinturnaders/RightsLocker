// jwt.interceptor.ts
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private auth = inject(AuthService);
  private refreshing = false;

  private queued: Array<{
    req: HttpRequest<any>;
    next: HttpHandler;
    resolve: (v: HttpEvent<any>) => void;  // <-- typed to HttpEvent
    reject: (e: any) => void;
  }> = [];

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const t = this.auth.token;
    const authed = t ? req.clone({ setHeaders: { Authorization: `Bearer ${t}` } }) : req;

    const run = (r: HttpRequest<any>): Observable<HttpEvent<any>> =>
      next.handle(r).pipe(
        catchError((err: HttpErrorResponse) => {
          if (err.status !== 401) {
            return throwError(() => err);   // Observable<never> is fine
          }

          if (!this.refreshing) {
            this.refreshing = true;

            return this.auth.refresh().pipe(            // Observable<string>
              switchMap((newT: string) => {             // <-- never null now
                this.refreshing = false;

                // Flush queued requests with the fresh token
                const pending = this.queued.splice(0);
                pending.forEach(job => {
                  const retriedJob = job.req.clone({ setHeaders: { Authorization: `Bearer ${newT}` } });
                  next.handle(retriedJob).subscribe(job.resolve, job.reject);
                });

                // Retry the original request
                const retried = req.clone({ setHeaders: { Authorization: `Bearer ${newT}` } });
                return next.handle(retried) as Observable<HttpEvent<any>>;
              }),
              catchError((e: any) => {
                this.refreshing = false;
                // Fail everyone waiting
                this.queued.splice(0).forEach(j => j.reject(e));
                return throwError(() => e);
              })
            );
          }

          // If a refresh is already in flight, queue this request
          return new Observable<HttpEvent<any>>((observer) => {
            this.queued.push({
              req, next,
              resolve: (v: HttpEvent<any>) => { observer.next(v); observer.complete(); },
              reject: (e: any) => observer.error(e)
            });
          });
        })
      );

    return run(authed);
  }
}
