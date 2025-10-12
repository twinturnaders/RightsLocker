import { Component, inject } from '@angular/core';
import { EvidenceApi, Evidence} from '../../../core/evidence.service/evidence.service.component';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, RouterLink, DatePipe],
  template: `
  <h2>Evidence</h2>
  <div class="grid" *ngIf="items as page">
    <div class="card" *ngFor="let e of page.content">
      <div class="row">
        <div class="title">{{e.title || 'Untitled'}}</div>
        <span class="status">{{e.status}}</span>
      </div>
      <div class="meta">Captured: {{e.capturedAt | date:'short'}} • Legal hold: {{e.legalHold}}</div>
      <a [routerLink]="['/evidence', e.id]">Open</a>
    </div>
  </div>
  `,
  styles: [`.grid{display:grid;gap:12px} .card{border:1px solid #eee;padding:12px;border-radius:8px} .row{display:flex;justify-content:space-between}`]
})
export class EvidenceListComponent {
  api = inject(EvidenceApi);
  items: any;
  ngOnInit(){ this.api.list({ page: 0, size: 20 }).subscribe(p => this.items = p); }
}
