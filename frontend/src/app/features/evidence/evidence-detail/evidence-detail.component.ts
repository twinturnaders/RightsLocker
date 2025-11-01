import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AsyncPipe, DatePipe, NgIf } from '@angular/common';
import {Evidence, EvidenceApi} from '../../../core/evidence.service';


@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, DatePipe],
  templateUrl: './evidence-detail.component.html',
})
export class EvidenceDetailComponent {
  route = inject(ActivatedRoute);
  api = inject(EvidenceApi);

  e?: Evidence;
  loading = false;
  error = '';

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.fetch(id);
  }

  fetch(id: string) {
    this.loading = true; this.error = '';
    this.api.get(id).subscribe({
      next: ev => { this.e = ev; this.loading = false; },
      error: err => { this.error = err?.error?.message || err.statusText || 'Failed to load'; this.loading = false; }
    });
  }

  toggleHold(evt: Event) {
    if (!this.e) return;
    const checked = (evt.target as HTMLInputElement).checked;
    this.api.setLegalHold(this.e.id, checked).subscribe({
      next: ev => this.e = ev,
      error: err => { (evt.target as HTMLInputElement).checked = !checked; /* rollback */ }
    });
  }

  download(type: 'redacted'|'thumbnail'|'original' = 'redacted') {
    if (!this.e) return;
    this.api.download(this.e.id, type).subscribe(({ url }) => window.open(url, '_blank'));
  }
}
