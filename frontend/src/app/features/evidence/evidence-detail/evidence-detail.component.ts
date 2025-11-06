import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { DatePipe, NgIf } from '@angular/common';
import { Evidence, EvidenceApi } from '../../../core/evidence.service';
import {environment} from '../../../../environments/environment';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, DatePipe],
  templateUrl: './evidence-detail.component.html',
})
export class EvidenceDetailComponent implements OnChanges {
  private api = inject(EvidenceApi);
  private base = `${environment.apiBase}`;
  @Input() id?: string;
  e?: Evidence;
  loading = false;
  error = '';

  ngOnChanges(ch: SimpleChanges) {
    if (ch['id'] && this.id) this.fetch(this.id);
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
      error: _ => { (evt.target as HTMLInputElement).checked = !checked; }
    });
  }

  thumbUrl(): string | undefined {
    return this.e?.id ? `/${this.base}/evidence/thumb?id=${this.e.id}` : undefined;
  }

  download(type: 'redacted'|'thumbnail'|'original' = 'redacted') {
    if (!this.e) return;
    this.api.download(this.e.id, type).subscribe(({ url }) => window.open(url, '_blank'));
  }
}
