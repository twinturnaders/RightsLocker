import { Routes } from '@angular/router';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './core/auth.service';

export const authGuard = () => {
  const auth = inject(AuthService);
  return auth.isAuthed$;
};

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'evidence', loadComponent: () => import('./features/evidence/evidence-list.component').then(m => m.EvidenceListComponent), canActivate: [authGuard] },
  { path: 'evidence/:id', loadComponent: () => import('./features/evidence/evidence-detail.component').then(m => m.EvidenceDetailComponent), canActivate: [authGuard] },
  { path: 'upload', loadComponent: () => import('./features/evidence/evidence-upload.component').then(m => m.EvidenceUploadComponent), canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'evidence' },
  { path: '**', redirectTo: 'evidence' }
];

export const routingProviders = [
  provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled' }))
];
