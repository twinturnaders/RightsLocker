import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent} from "./app/app.component";
import { routingProviders} from "./app/app.routes";
import {ApplicationRef, importProvidersFrom} from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import {AuthService} from './app/core/auth.service/auth.service.component';

bootstrapApplication(AppComponent, {
    providers: [
        routingProviders,
        importProvidersFrom(HttpClientModule),
    ],
}).catch(err => console.error(err)).then(ref => {
  if (ref instanceof ApplicationRef) {
    const auth = ref.injector.get(AuthService);
    auth.init();
  }
});
