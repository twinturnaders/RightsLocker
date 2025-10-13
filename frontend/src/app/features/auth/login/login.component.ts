import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService} from '../../../core/auth.service';
import { NgIf } from '@angular/common';

@Component({
  standalone: true,
  selector: 'rl-login',
  imports: [ReactiveFormsModule, NgIf],
  templateUrl: 'login.component.html',
  styleUrls: ['login.component.css']
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
