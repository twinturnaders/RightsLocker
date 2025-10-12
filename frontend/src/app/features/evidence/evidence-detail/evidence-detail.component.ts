import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AsyncPipe, DatePipe, NgIf } from '@angular/common';
import {Evidence, EvidenceApi} from '../../../core/evidence.service/evidence.service.component';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, DatePipe],
  template: `
  <ng-container *ngIf="e as ev">
    <h2>{{ev.title || 'Untitled'}}</h2>
    <p>{{ev.description}}</p>
    <p>Captured: {{ev.capturedAt | date:'long'}}</p>
    <p>Status: <b>{{ev.status}}</b></p>
    <label><input type="checkbox" [checked]="ev.legalHold" (change)="toggleHold($event)"> Legal Hold</label>
    <div style="margin-top:12px">
      <button (click)="download()">Download</button>
    </div>
  </ng-container>
  `
})
export class EvidenceDetailComponent {
  route = inject(ActivatedRoute);
  api = inject(EvidenceApi);
  e?: Evidence;
//thumbById(id: string){ return `/api/evidence/thumb?id=${encodeURIComponent(id)}`; }
  ngOnInit(){
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get(id).subscribe(ev => this.e = ev);
  }

  toggleHold(evt: Event){
    if (!this.e) return; const checked = (evt.target as HTMLInputElement).checked;
    this.api.setLegalHold(this.e.id, checked).subscribe(ev => this.e = ev);
  }

  download(){ if (!this.e) return; this.api.download(this.e.id).subscribe(({url}) => window.open(url, '_blank')); }
}
