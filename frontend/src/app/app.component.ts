// shell/app.component.ts (sidebar + topbar)
import { Component, inject } from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AsyncPipe, NgIf} from '@angular/common';
import { AuthService} from './core/auth.service';

@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive],
  templateUrl: `app.component.html`,
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  auth = inject(AuthService);
}
