import { Injectable, signal } from '@angular/core';
export interface Toast { id:number; kind:'success'|'error'|'info'; text:string; ttl?:number }
@Injectable({providedIn:'root'})
export class ToastService{
  list = signal<Toast[]>([]); private id = 1;
  private push(kind:Toast['kind'], text:string, ttl=3500){ const t={id:this.id++,kind,text,ttl}; this.list.update(v=>[...v,t]); setTimeout(()=>this.dismiss(t.id), ttl); }
  success(m:string){ this.push('success', m); } error(m:string){ this.push('error', m); } info(m:string){ this.push('info', m); }
  dismiss(id:number){ this.list.update(v=>v.filter(x=>x.id!==id)); }
}
