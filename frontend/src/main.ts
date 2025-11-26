import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent} from './app/app.component';
import { routingProviders} from "./app/app.routes";
import { importProvidersFrom } from '@angular/core';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {JwtInterceptor} from './app/core/jwt.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    routingProviders,
    importProvidersFrom(HttpClientModule),
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
}).catch(err => console.error(err));
