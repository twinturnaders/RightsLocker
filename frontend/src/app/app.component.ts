
import {Component, inject, OnInit} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AsyncPipe, CommonModule, NgIf} from '@angular/common';
import { AuthService} from "./core/auth.service/auth.service.component";
import {ToastContainer} from './ui/toast.container/toast.container.component';
import {SiteHeaderComponent} from './core/app-header/app-header.component';


@Component({
  standalone: true,
  selector: 'rl-root',
  imports: [CommonModule, RouterOutlet, RouterLink, NgIf, AsyncPipe, RouterLinkActive, ToastContainer, SiteHeaderComponent],
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"]
})
export class AppComponent {
  auth = inject(AuthService);


  onLogout() {
    return this.auth;
  }
}
