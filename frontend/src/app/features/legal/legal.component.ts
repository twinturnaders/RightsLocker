import { Component, signal } from '@angular/core';
import { NgIf } from '@angular/common';
import {FormsModule} from '@angular/forms';

@Component({
  standalone: true,
  selector: 'rl-legal',
  imports: [ NgIf, FormsModule],
  templateUrl: 'legal.component.html',
  styleUrls: ['legal.component.css']
})
export class LegalComponent {
  tab = signal<'federal'|'state'>('federal');
  q = '';
  states = [
    { code:'CO', name:'Colorado' }, { code:'CA', name:'California' }, { code:'TX', name:'Texas' },
    { code:'NY', name:'New York' }, { code:'FL', name:'Florida' }
  ];
}
