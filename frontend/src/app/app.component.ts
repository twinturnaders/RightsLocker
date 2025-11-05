
import { Component, inject } from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AsyncPipe, NgIf} from '@angular/common';
import { AuthService} from './core/auth.service';

@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive],
  templateUrl: `app.component.html`,
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  get auth(): AuthService {
    return this._auth;
  }
  private _auth = inject(AuthService);
}
