import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './shell/app.component';
import { importProvidersFrom } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { routingProviders } from './app.routes';
import { JwtInterceptor } from './core/jwt.interceptor';
import { provideAnimations } from '@angular/platform-browser/animations';

bootstrapApplication(AppComponent, {
  providers: [
    importProvidersFrom(HttpClientModule),
    routingProviders,
    provideAnimations(),
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
  ]
}).catch(err => console.error(err));
