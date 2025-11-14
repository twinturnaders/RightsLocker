import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, catchError, map, of, pipe, switchMap, tap} from 'rxjs';
import { environment } from '../../environments/environment';


interface LoginRes{ accessToken:string, email:string }
interface RegisterReq{ email:string; password:string; displayName:string }
interface LoginRequest { email: string; password: string; }

@Injectable({providedIn:'root'})
export class AuthService{
  private http=inject(HttpClient);
  private _token$=new BehaviorSubject<string|null>(null);


  isAuthed$=this._token$.pipe(map(t=>!!t));
  get token(){return this._token$.value}
  set token(v){this._token$.next(v)}



  login(email:string,password:string){

    return this.http.post<LoginRes>(`${environment.apiBase}/auth/login`,{email,password},{withCredentials:true})
      .pipe(tap(r=>this.token=r.accessToken))

  }
  register(email:string,password:string,displayName:string){
    const body: RegisterReq={email,password,displayName};
    return this.http.post(`${environment.apiBase}/auth/register`,body,{withCredentials:true})
      .pipe(switchMap(()=>this.login(email,password)));
  }
  refresh(){
    // call /api/auth/refresh with current token as Bearer (acting as refresh for now)
    const token = this.token;
    return this.http.post<LoginRes>(
      `${environment.apiBase}/auth/refresh`,
      {},
      { headers: token ? { Authorization: `Bearer ${token}` } : {} }
    ).pipe(
      tap(r => this.token = r.accessToken),
      map(r=>r.accessToken),
    );
  }
  logout(){ this.http.post(`${environment.apiBase}/auth/logout`,{}, {withCredentials:true}).subscribe(); this.token=null; }
  demoLogin(){ this.login('demo@rl.local','password').subscribe(); }

}
