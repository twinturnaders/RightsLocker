
import {Component, inject, signal} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AsyncPipe, CommonModule, NgIf, NgOptimizedImage} from '@angular/common';
import { AuthService} from './core/auth.service';
import {LogoComponent} from './core/logo/logo.component';

@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [CommonModule, RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive, NgOptimizedImage, LogoComponent],
  templateUrl: `app.component.html`,
  styleUrls: ['./app.component.css'],
})
export class AppComponent {

  protected auth = inject(AuthService);
  sidebarOpen = signal(false);

  toggleSidebar() { this.sidebarOpen.update(v => !v); }
  closeSidebar() { this.sidebarOpen.set(false); }
}
