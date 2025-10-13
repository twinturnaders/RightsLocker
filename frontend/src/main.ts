import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent} from './app/app.component';
import { routingProviders} from "./app/app.routes";
import { importProvidersFrom } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

bootstrapApplication(AppComponent, {
    providers: [
        routingProviders,
        importProvidersFrom(HttpClientModule),
    ],
}).catch(err => console.error(err));
