import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent} from './app/app.component';
import { routingProviders} from "./app/app.routes";
import { importProvidersFrom } from '@angular/core';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {AuthInterceptor} from './app/core/auth.interceptor';

bootstrapApplication(AppComponent, {
    providers: [
        routingProviders,
        importProvidersFrom(HttpClientModule),
      {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true}
    ],
}).catch(err => console.error(err));
