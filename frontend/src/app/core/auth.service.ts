import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, map, Observable, throwError} from 'rxjs';
import { environment } from '../../environments/environment';
import {switchMap} from 'rxjs/operators';

interface RegisterReq{ email:string; password:string; displayName:string }

@Injectable({providedIn:'root'})
export class AuthService {
  private http = inject(HttpClient);
  private _token$ = new BehaviorSubject<string | null>(null);
  private _refreshToken$ = new BehaviorSubject<string | null>(null);

  constructor() {
    const t = localStorage.getItem('rl.access');
    const r = localStorage.getItem('rl.refresh');
    if (t) this._token$.next(t);
    if (r) this._refreshToken$.next(r);
  }

  get token() {
    return this._token$.value;
  }

  set token(v: string | null) {
    this._token$.next(v);
    if (v) {
      localStorage.setItem('rl.access', v);
    } else {
      localStorage.removeItem('rl.access');
    }
  }

  get refreshToken() {
    return this._refreshToken$.value;
  }

  isAuthed$ = this._token$.pipe(map(t => !!t));

  private setTokens(access: string, refresh: string) {
    // keep these two in sync
    this.token = access;
    this._refreshToken$.next(refresh);
    localStorage.setItem('rl.refresh', refresh);
  }

  login(email: string, password: string) {
    return this.http.post<{ accessToken: string, refreshToken: string, email: string }>(
      `${environment.apiBase}/auth/login`,
      {email, password},
      {withCredentials: true}
    ).pipe(
      map(r => {
        this.setTokens(r.accessToken, r.refreshToken);
        return r;
      })
    );
  }

  register(email:string,password:string,displayName:string){
    const body: RegisterReq = {email,password,displayName};
    return this.http.post(
      `${environment.apiBase}/auth/register`,
      body,
      {withCredentials:true}
    ).pipe(
      // auto-login after register
      switchMap(() => this.login(email, password))
    );
  }

  /** Use the REFRESH token for the refresh endpoint */
  refresh(): Observable<string> {
    const rt = this.refreshToken;
    if (!rt) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<{ accessToken: string; refreshToken?: string }>(
      `${environment.apiBase}/auth/refresh`,
      {},
      {
        headers: { Authorization: `Bearer ${rt}` }
      }
    ).pipe(
      map(r => {
        if (!r?.accessToken) throw new Error('No access token in refresh response');

        if (r.refreshToken) {
          // if backend rotates refresh tokens
          this.setTokens(r.accessToken, r.refreshToken);
        } else {
          // just update access
          this.token = r.accessToken;
        }

        return r.accessToken;
      })
    );
  }

  logout() {
    this.http.post(`${environment.apiBase}/auth/logout`, {}, {withCredentials: true})
      .subscribe({ error: () => {} });

    this._token$.next(null);
    this._refreshToken$.next(null);
    localStorage.removeItem('rl.access');
    localStorage.removeItem('rl.refresh');
  }
}
