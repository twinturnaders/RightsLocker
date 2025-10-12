import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, EMPTY, Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { environment} from '../../../environments/environment';

interface LoginReq { email: string; password: string; }
interface LoginRes { accessToken: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private _token$ = new BehaviorSubject<string | null>(null);
  isAuthed$ = this._token$.pipe(map(t => !!t));

  get token() { return this._token$.value; }
  set token(v: string | null) { this._token$.next(v); }

  login(email: string, password: string) {
    return this.http.post<LoginRes>(`${environment.apiBase}/api/auth/login`, { email, password }, { withCredentials: true })
      .pipe(tap(res => this.token = res.accessToken));
  }

  demoLogin(){
    // quick helper to test without a UI form
    return this.login('demo@rl.local','password').subscribe();
  }

  refresh(): Observable<string | null> {
    return this.http.post<LoginRes>(`${environment.apiBase}/api/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(res => this.token = res.accessToken),
        map(res => res.accessToken),
        catchError(() => of(null))
      );
  }

  logout(){
    this.http.post(`${environment.apiBase}/api/auth/logout`, {}, { withCredentials: true }).subscribe();
    this.token = null;
  }
}
