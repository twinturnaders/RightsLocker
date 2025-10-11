import {booleanAttribute, Component, EventEmitter, inject, Inject, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {AuthService} from '../auth.service/auth.service.component';

@Component({
  selector: 'app-site-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './app-header.component.html',
  styleUrls: ['./app-header.component.css']

})
export class SiteHeaderComponent {
  auth = inject(AuthService);
  @Input({transform: booleanAttribute}) isAuthed = false;
  @Output() logout = new EventEmitter<void>();
  menuOpen = false;

  checkAuth() {
    if (this.auth.isAuthed$){
      this.isAuthed = true;
    }
  }
  closeMenu() { this.menuOpen = false; }
}
