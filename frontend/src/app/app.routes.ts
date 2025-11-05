import { Routes, provideRouter } from '@angular/router';
import {EvidencePageComponent} from './features/evidence/evidence-page/evidence-page.component';


export const routes: Routes = [
  { path:'', pathMatch:'full', redirectTo:'evidence' },
  { path:'convert', loadComponent:()=>import('./features/evidence/convert/convert.component').then(m=>m.ConvertComponent) },
  { path:'about', loadComponent:()=>import('./features/about/about.component').then(m=>m.AboutComponent) },
// keep your existing protected routes below (evidence, upload, detail)
  { path:'legal', loadComponent:()=>import('./features/legal/legal.component').then(m=>m.LegalComponent) },

  { path:'register', loadComponent:()=>import('./features/auth/register/register.component').then(m=>m.RegisterComponent) },
  { path: 'evidence', component: EvidencePageComponent },
  { path: 'evidence/:id', loadComponent: () => import('./features/evidence/evidence-detail/evidence-detail.route-wrapper').then(m => m.EvidenceDetailRouteWrapper) },
  { path: 'upload', redirectTo: 'evidence' },
  { path:'**', redirectTo:'evidence' }
];
export const routingProviders=[provideRouter(routes)];
