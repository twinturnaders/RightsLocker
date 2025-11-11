import {Component, inject, signal} from '@angular/core';
import {AnonComponent} from '../anon-upload/anon-upload.component';
import {ConvertComponent} from '../evidence/convert/convert.component';
import {Router, RouterLink} from '@angular/router';
import {NgIf} from '@angular/common';
import {Evidence} from '../../core/evidence.service';

@Component({
  standalone: true,
  selector: 'rl-home',
  imports: [NgIf, AnonComponent, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {

}
