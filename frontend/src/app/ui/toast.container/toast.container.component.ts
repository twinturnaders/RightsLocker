import { Component, effect, inject } from '@angular/core';
import { NgFor } from '@angular/common';
import { ToastService} from '../toast.service';
import {} from '../../app.component';


@Component({
  standalone:true,
  selector:'rl-toasts',
  imports:[NgFor],
  template:`
<div class="toasts">
<div class="toast" *ngFor="let t of svc.list()" [class.ok]="t.kind==='success'" [class.err]="t.kind==='error'" [class.info]="t.kind==='info'">
<span class="dot"></span>
<div class="msg">{{t.text}}</div>
<button class="x" (click)="svc.dismiss(t.id)">×</button>
</div>
</div>
`,
  styles:[`
.toasts{position:fixed;right:16px;bottom:16px;display:flex;flex-direction:column;gap:8px;z-index:1000}
.toast{display:flex;align-items:center;gap:10px;min-width:280px;max-width:420px;background:var();border:1px solid #203044;padding:10px 12px;border-radius:12px;color:#e5e7eb;box-shadow:0 10px 30px rgba(0,0,0,.35)}
.toast.ok{border-color:#14532d}.toast.err{border-color:#7f1d1d}.toast.info{border-color:#0c4a6e}
.dot{width:8px;height:8px;border-radius:999px;background:var()}
.toast.ok .dot{background:#22c55e}.toast.err .dot{background:#ef4444}.toast.info .dot{background:#38bdf8}
.x{margin-left:auto;background:transparent;border:0;color:#9aa4b2;font-size:18px;cursor:pointer}
`]
})
export class ToastContainer{ svc = inject(ToastService); constructor(){ effect(()=>this.svc.list()); } }
