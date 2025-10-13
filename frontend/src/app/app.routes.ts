import { Routes, provideRouter } from '@angular/router';


export const routes: Routes = [
  { path:'', pathMatch:'full', redirectTo:'convert' },
  { path:'convert', loadComponent:()=>import('./features/public/convert/convert.component').then(m=>m.ConvertComponent) },
  { path:'about', loadComponent:()=>import('./features/about/about.component').then(m=>m.AboutComponent) },
// keep your existing protected routes below (evidence, upload, detail)
  { path:'register', loadComponent:()=>import('./features/auth/register/register.component').then(m=>m.RegisterComponent) },
  { path:'evidence', loadComponent:()=>import('./features/evidence/evidence-list/evidence-list.component').then(m=>m.EvidenceListComponent) },
  { path:'evidence/:id', loadComponent:()=>import('./features/evidence/evidence-detail/evidence-detail.component').then(m=>m.EvidenceDetailComponent) },
  { path:'upload', loadComponent:()=>import('./features/evidence/evidence-upload/evidence-upload.component').then(m=>m.EvidenceUploadComponent) },
  { path:'**', redirectTo:'convert' }
];
export const routingProviders=[provideRouter(routes)];
