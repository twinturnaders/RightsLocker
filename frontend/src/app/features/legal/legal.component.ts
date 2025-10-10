import { Component, signal } from '@angular/core';
import { NgIf } from '@angular/common';
import {FormsModule} from '@angular/forms';

@Component({
  standalone: true,
  selector: 'rl-legal',
  imports: [ NgIf, FormsModule],
  template: `
  <h2>Laws & Guidance</h2>
  <div class="tabs">
    <button [class.active]="tab()=='federal'" (click)="tab.set('federal')">Federal</button>
    <button [class.active]="tab()=='state'" (click)="tab.set('state')">State</button>
  </div>

  <section *ngIf="tab()=='federal'">
    <div class="card">
      <h3>Evidence Authentication & Chain of Custody</h3>
      <ul>
        <li>FRE 901 – Authenticating or Identifying Evidence</li>
        <li>FRE 902(13)/(14) – Certified Records; Data Copied from Electronic Device</li>
        <li>FRE 403 – Excluding Relevant Evidence (prejudice vs probative)</li>
      </ul>
      <p class="note">Add citations/links later; this is a placeholder summary.</p>
    </div>
  </section>

<!--  <section *ngIf="tab()=='state'">-->
<!--    <input placeholder="Filter by state…" [(ngModel)]="q" />-->
<!--    <div class="accordion">-->
<!--      <details *ngFor="let s of states | stateFilter:q">-->
<!--        <summary>{{s.name}}</summary>-->
        <div class="card">
          <p class="muted">Add the specific statute/rule citations here (e.g., Evidence Rule 901 analogs, public records redaction rules, bodycam statutes).</p>
        </div>
<!--      </details>-->
<!--    </div>-->
<!--  </section>-->
  `,
  styles:[`
    .tabs{display:flex;gap:8px;margin:8px 0} .tabs button{padding:8px 12px;border:1px solid #ddd;border-radius:8px;background:#fff}
    .tabs .active{background:#111;color:#fff;border-color:#111}
    .card{border:1px solid #eee;border-radius:8px;padding:12px;margin:8px 0}
    .muted{color:#666} .note{font-size:12px;color:#666}
    input{padding:8px;border:1px solid #ddd;border-radius:8px;margin:8px 0;width:280px}
  `]
})
export class LegalComponent {
  tab = signal<'federal'|'state'>('federal');
  q = '';
  states = [
    { code:'CO', name:'Colorado' }, { code:'CA', name:'California' }, { code:'TX', name:'Texas' },
    { code:'NY', name:'New York' }, { code:'FL', name:'Florida' }
  ];
}
