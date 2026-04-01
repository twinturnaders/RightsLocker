import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnonComponent } from '../anon-upload/anon-upload.component';

@Component({
  standalone: true,
  selector: 'rl-home',
  imports: [AnonComponent, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {}
