import { Routes, provideRouter } from '@angular/router';
import {EvidencePageComponent} from './features/evidence/evidence-page/evidence-page.component';


export const routes: Routes = [
  { path:'', pathMatch:'full', redirectTo:'upload' },
  { path:'upload', loadComponent:()=>import('./features/anon-upload/anon-upload.component').then(m=>m.AnonComponent) },

  { path:'convert', loadComponent:()=>import('./features/evidence/convert/convert.component').then(m=>m.ConvertComponent) },
  { path:'about', loadComponent:()=>import('./features/about/about.component').then(m=>m.AboutComponent) },
// keep your existing protected routes below (evidence, upload, detail)
  { path:'legal', loadComponent:()=>import('./features/legal/legal.component').then(m=>m.LegalComponent) },
  { path: 'convert', loadComponent: () => import('./features/evidence/convert/convert.component').then(m => m.ConvertComponent) },
  { path: 'evidence', loadComponent: () => import('./features/evidence/evidence-page/evidence-page.component').then(m => m.EvidencePageComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },

  { path:'register', loadComponent:()=>import('./features/auth/register/register.component').then(m=>m.RegisterComponent) },
  { path: 'evidence', component: EvidencePageComponent },
  { path: 'evidence/:id', loadComponent: () => import('./features/evidence/evidence-detail/evidence-detail.route-wrapper').then(m => m.EvidenceDetailRouteWrapper) },
  { path: 'convert', redirectTo: 'evidence' },
  { path:'**', redirectTo:'upload' }
];
export const routingProviders=[provideRouter(routes)];
