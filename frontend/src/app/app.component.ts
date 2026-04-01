import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AsyncPipe, CommonModule, NgIf } from '@angular/common';
import { AuthService } from './core/auth.service';

@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [CommonModule, RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive],
  templateUrl: `app.component.html`,
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  auth = inject(AuthService);
  sidebarOpen = signal(false);

  toggleSidebar() {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
  }
}
