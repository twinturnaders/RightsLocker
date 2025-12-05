import { Routes, provideRouter } from '@angular/router';
import {EvidencePageComponent} from './features/evidence/evidence-page/evidence-page.component';
import {HomeComponent} from './features/home/home.component';
import {PrivacyComponent} from './features/legal/privacy/privacy.component';
import {EvidenceDetailRouteWrapper} from './features/evidence/evidence-detail/evidence-detail.route-wrapper';
import {EvidenceDetailComponent} from './features/evidence/evidence-detail/evidence-detail.component';
import {EvidenceListComponent} from './features/evidence/evidence-list/evidence-list.component';



export const routes: Routes = [
  { path:'', pathMatch:'full', redirectTo:'home' },
  {path: 'home', loadComponent:()=>import('./features/home/home.component').then(m => m.HomeComponent)},
  { path:'upload', loadComponent:()=>import('./features/anon-upload/anon-upload.component').then(m=>m.AnonComponent) },
  { path: 'legal/privacy', loadComponent:()=>import('./features/legal/privacy/privacy.component').then(m=>m.PrivacyComponent) },
  { path: 'legal/terms', loadComponent:()=>import('./features/legal/tos/tos.component').then(m=>m.TosComponent) },
  { path:'convert', loadComponent:()=>import('./features/evidence/convert/convert.component').then(m=>m.ConvertComponent) },
  { path:'about', loadComponent:()=>import('./features/about/about.component').then(m=>m.AboutComponent) },
// keep your existing protected routes below (evidence, upload, detail)
  { path:'legal', loadComponent:()=>import('./features/legal/legal.component').then(m=>m.LegalComponent) },
  { path: 'convert', loadComponent: () => import('./features/evidence/convert/convert.component').then(m => m.ConvertComponent) },
  { path: 'evidence', loadComponent: () => import('./features/evidence/evidence-page/evidence-page.component').then(m => m.EvidencePageComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },

  { path:'register', loadComponent:()=>import('./features/auth/register/register.component').then(m=>m.RegisterComponent) },
  { path: 'evidence', component: EvidencePageComponent },
  { path: 'evidence-list', loadComponent: () => import('./features/evidence/evidence-list/evidence-list.component').then(m => m.EvidenceListComponent) },
  { path: 'evidence/:id', loadComponent: () => import('./features/evidence/evidence-detail/evidence-detail.route-wrapper').then(m => m.EvidenceDetailRouteWrapper) },
  { path: 'convert', redirectTo: 'evidence' },
  { path: 'evidence/:id', component: EvidencePageComponent },
  { path: 'evidence-list', component: EvidencePageComponent },
  { path:'**', redirectTo:'home' }
];
export const routingProviders=[provideRouter(routes)];
