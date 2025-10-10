import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AsyncPipe, NgIf } from '@angular/common';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'rl-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, AsyncPipe, NgIf],
  template: `
  <header class="hdr">
    <a routerLink="/evidence" class="brand">RightsLocker</a>
    <nav>
      <a routerLink="/upload" *ngIf="auth.isAuthed$ | async">Upload</a>
      <a routerLink="/evidence" *ngIf="auth.isAuthed$ | async">Evidence</a>
    </nav>
    <div class="spacer"></div>
    <button *ngIf="!(auth.isAuthed$ | async)" (click)="auth.demoLogin()">Demo Login</button>
    <button *ngIf="auth.isAuthed$ | async" (click)="auth.logout()">Logout</button>
  </header>
  <main><router-outlet></router-outlet></main>
  `,
  styles: [`.hdr{display:flex;gap:16px;align-items:center;padding:12px;border-bottom:1px solid #eee}.brand{font-weight:700}.spacer{flex:1}`]
})
export class AppComponent {
  auth = inject(AuthService);
}
