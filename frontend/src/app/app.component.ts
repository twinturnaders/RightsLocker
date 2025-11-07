
import { Component, inject } from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AsyncPipe, NgIf, NgOptimizedImage} from '@angular/common';
import { AuthService} from './core/auth.service';
import {LogoComponent} from './core/logo/logo.component';

@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive, NgOptimizedImage, LogoComponent],
  templateUrl: `app.component.html`,
  styleUrls: ['./app.component.css'],
})
export class AppComponent {

  protected auth = inject(AuthService);
  
}
