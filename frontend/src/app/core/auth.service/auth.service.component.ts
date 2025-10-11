import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { environment} from '../../../environments/environment.prod';

interface LoginReq { email: string; password: string; }
interface LoginRes { accessToken: string; }
interface RegisterReq { email: string; password: string; displayName: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private _token$ = new BehaviorSubject<string | null>(null);
  isAuthed$ = this._token$.pipe(map(t => !!t));
  isAuthenticated: boolean = false;

  get token() { return this._token$.value; }
  set token(v: string | null) { this._token$.next(v); }

  /** Call once on app start if you want silent login */
  init() {
    this.refresh().subscribe(); // no-op if cookie absent
  }

  login(email: string, password: string) {
    return this.http.post<LoginRes>(`${environment.apiBase}/auth/login`, { email, password }, { withCredentials: true })
      .pipe(tap(res => this.token = res.accessToken));
  }

  /** Create user and automatically log them in */
  register(email: string, password: string, displayName: string) {
    const body: RegisterReq = { email, password, displayName };
    return this.http.post(`${environment.apiBase}/auth/register`, body, { withCredentials: true }).pipe(
      switchMap(() => this.login(email, password))
    );
  }

  refresh(): Observable<string | null> {
    return this.http.post<LoginRes>(`${environment.apiBase}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(res => this.token = res.accessToken),
        map(res => res.accessToken),
        catchError(() => of(null))
      );
  }

  onLogout(){
    this.http.post(`${environment.apiBase}/auth/logout`, {}, { withCredentials: true }).subscribe();
    this.token = null;
  }

  demoLogin(){ this.login('demo@rl.local','password').subscribe(); }

  authCheck() {
  if (this.token != null) {
    this.isAuthenticated = true;
  }
  }
}
