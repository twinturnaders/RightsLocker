import { Routes, provideRouter } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService} from './core/auth.service/auth.service.component';

const authed = () => inject(AuthService).isAuthed$;

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'evidence' },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'evidence', loadComponent: () => import('./features/evidence/evidence-list/evidence-list.component').then(m => m.EvidenceListComponent), canActivate:[authed]},
  { path: 'evidence/:id', loadComponent: () => import('./features/evidence/evidence-detail/evidence-detail.component').then(m => m.EvidenceDetailComponent), canActivate:[authed]},
  { path: 'upload', loadComponent: () => import('./features/evidence/evidence-upload/evidence-upload.component').then(m => m.EvidenceUploadComponent), canActivate:[authed]},
  { path: 'legal', loadComponent: () => import('./features/legal/legal.component').then(m => m.LegalComponent) },
  { path: 'about', loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent) },
  { path: '**', redirectTo: 'evidence' }
];
export const routingProviders = [provideRouter(routes)];
