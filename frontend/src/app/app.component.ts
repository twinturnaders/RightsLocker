// shell/app.component.ts (sidebar + topbar)
import { Component, inject } from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AsyncPipe, NgIf} from '@angular/common';
import { AuthService} from "./core/auth.service/auth.service.component";

@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive],
  template: `
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">RightsLocker</div>
      <nav>
        <a routerLink="/evidence" routerLinkActive="active">Evidence</a>
        <a routerLink="/upload" routerLinkActive="active">Upload</a>
        <a routerLink="/legal" routerLinkActive="active">Laws & Guidance</a>
        <a routerLink="/about" routerLinkActive="active">About</a>
      </nav>
    </aside>

    <section class="main">
      <header class="topbar">
        <input placeholder="Search evidence…" />
        <div class="spacer"></div>
        <button *ngIf="!(auth.isAuthed$ | async)" (click)="auth.demoLogin()">Demo Login</button>
        <button *ngIf="auth.isAuthed$ | async" (click)="auth.logout()">Logout</button>
      </header>
      <div class="content"><router-outlet></router-outlet></div>
    </section>
  </div>
  `,
  styles:[`
    .layout{display:grid;grid-template-columns:240px 1fr;height:100vh}
    .sidebar{border-right:1px solid #eee;padding:16px;display:flex;flex-direction:column;gap:8px}
    .brand{font-weight:700;margin-bottom:8px}
    .sidebar a{display:block;padding:8px;border-radius:8px;color:#222;text-decoration:none}
    .sidebar a.active, .sidebar a:hover{background:#f5f5f5}
    .topbar{display:flex;align-items:center;gap:12px;padding:12px;border-bottom:1px solid #eee}
    .topbar input{width:360px;max-width:40vw;padding:8px 10px;border:1px solid #ddd;border-radius:8px}
    .spacer{flex:1}
    .content{padding:16px;overflow:auto;height:calc(100vh - 54px)}
  `]
})
export class AppComponent {
  auth = inject(AuthService);
}
