import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, catchError, map, Observable, of, pipe, switchMap, tap} from 'rxjs';
import { environment } from '../../environments/environment';
import { JwtInterceptor} from './jwt.interceptor';


interface LoginRes{ accessToken:string, email:string }
interface RegisterReq{ email:string; password:string; displayName:string }
interface LoginRequest { email: string; password: string; }

@Injectable({providedIn:'root'})
export class AuthService {
  private http = inject(HttpClient);
  private _token$ = new BehaviorSubject<string | null>(null);
  private _refreshToken$ = new BehaviorSubject<string | null>(null);

  constructor() {
    const t = localStorage.getItem('rl.access');     // restore after refresh
    const r = localStorage.getItem('rl.refresh');
    if (t) this._token$.next(t);
    if (r) this._refreshToken$.next(r);
  }

  get token() {
    return this._token$.value
  }
  set token(v: string | null) {
    this._token$.next(v);
  }

  isAuthed$ = this._token$.pipe(map(t => !!t));
  get refreshToken() {
    return this._refreshToken$.value
  }

  private setTokens(access: string, refresh: string) {
    this._token$.next(access);
    this._refreshToken$.next(refresh);
    localStorage.setItem('rl.access', access);
    localStorage.setItem('rl.refresh', refresh);
  }

  login(email: string, password: string) {
    return this.http.post<{ accessToken: string, refreshToken: string, email: string }>(
      `${environment.apiBase}/auth/login`,
      {email, password},
      {withCredentials: true}
    ).pipe(tap(r => this.setTokens(r.accessToken, r.refreshToken)));
  }

  // auth.service.ts
  refresh(): Observable<string> {
    const token = this.token;
    return this.http.post<{ accessToken: string }>(
      `${environment.apiBase}/auth/refresh`,
      {},
      { headers: token ? { Authorization: `Bearer ${token}` } : {} }
    ).pipe(
      map(r => {
        if (!r?.accessToken) throw new Error('No access token in refresh response');
        this.token = r.accessToken;          // also updates BehaviorSubject via setter
        return r.accessToken;                // <-- always a string now
      })
    );
  }


  logout() {
    this.http.post(`${environment.apiBase}/auth/logout`, {}, {withCredentials: true}).subscribe();
    this._token$.next(null);
    this._refreshToken$.next(null);
    localStorage.removeItem('rl.access');
    localStorage.removeItem('rl.refresh');
  }
}
