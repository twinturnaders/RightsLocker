import { Routes, provideRouter } from '@angular/router';
import { AuthGuard } from './core/auth.guard';
import { GuestGuard } from './core/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: 'home', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  {
    path: 'upload',
    loadComponent: () => import('./features/anon-upload/anon-upload.component').then(m => m.AnonComponent),
    canActivate: [GuestGuard]
  },
  { path: 'legal', loadComponent: () => import('./features/legal/legal.component').then(m => m.LegalComponent) },
  { path: 'legal/privacy', loadComponent: () => import('./features/legal/privacy/privacy.component').then(m => m.PrivacyComponent) },
  { path: 'legal/terms', loadComponent: () => import('./features/legal/tos/tos.component').then(m => m.TosComponent) },
  { path: 'about', loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent) },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [GuestGuard]
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [GuestGuard]
  },
  {
    path: 'evidence',
    loadComponent: () => import('./features/evidence/evidence-page/evidence-page.component').then(m => m.EvidencePageComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'evidence-list',
    loadComponent: () => import('./features/evidence/evidence-list/evidence-list.component').then(m => m.EvidenceListComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'evidence/:id',
    loadComponent: () => import('./features/evidence/evidence-detail/evidence-detail.route-wrapper').then(m => m.EvidenceDetailRouteWrapper),
    canActivate: [AuthGuard]
  },
  { path: 'convert', redirectTo: 'evidence' },
  { path: '**', redirectTo: 'home' }
];

export const routingProviders = [provideRouter(routes)];
