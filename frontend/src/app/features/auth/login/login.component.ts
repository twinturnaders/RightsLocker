import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService} from '../../../core/auth.service/auth.service.component';
import { NgIf } from '@angular/common';

@Component({
  standalone: true,
  selector: 'rl-login',
  imports: [ReactiveFormsModule, NgIf],
  template: `
  <h2>Login</h2>
  <form [formGroup]="form" (ngSubmit)="submit()">
    <label>Email<input formControlName="email" type="email" required></label>
    <label>Password<input formControlName="password" type="password" required></label>
    <button type="submit" [disabled]="form.invalid || loading">Login</button>
  </form>
  <p *ngIf="error" style="color:#c00">{{error}}</p>
  `,
  styles: ["label{display:block;margin:8px 0} input{margin-left:8px}"]
})
export class LoginComponent {
  fb = inject(FormBuilder);
  auth = inject(AuthService);
  router = inject(Router);
  loading = false; error = '';
  form = this.fb.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit(){
    if (this.form.invalid) return;
    this.loading = true; this.error = '';
    const { email, password } = this.form.value as any;
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigateByUrl('/evidence'),
      error: e => { this.error = e?.error?.message || 'Login failed'; this.loading = false; },
    });
  }
}
