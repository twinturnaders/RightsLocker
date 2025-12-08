import {Component, inject, signal, computed, Input, WritableSignal} from '@angular/core';
import { EvidenceApi, Evidence } from '../../../core/evidence.service';
import { HttpClient } from '@angular/common/http';
import {Router, RouterLink} from '@angular/router';
import { environment } from '../../../../environments/environment';
import {DatePipe, DecimalPipe, NgIf, NgOptimizedImage} from '@angular/common';
import {BytesPipe} from '../../../core/pipes/bytes.pipe';
import {DurationMsPipe} from '../../../core/pipes/duration.pipe';
import {SafeValue} from '@angular/platform-browser';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [
    DatePipe,
    BytesPipe,
    NgIf,
    DurationMsPipe,
    DecimalPipe,
    NgOptimizedImage,
    RouterLink,
    /* ... */],
  templateUrl: './evidence-detail.component.html',
  styleUrls: ['./evidence-detail.component.css']
})
export class EvidenceDetailComponent {
  private http = inject(HttpClient);
  private router = inject(Router);
  private api = inject(EvidenceApi);
  base = `${environment.apiBase}/evidence`;
  showSuccessMessage: WritableSignal<boolean> = signal(false);
  evidence = signal<Evidence | null>(null);
  loading = signal(false);
  deleting = signal(false);
  error = signal<string>('');

  @Input() set id(value: string | undefined) {
    if (!value) return;
    this.load(value);
  }

  private load(id: string) {
    this.loading.set(true);
    this.error.set('');
    this.api.get(id).subscribe({
      next: ev => {
        this.evidence.set(ev);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err?.error?.message || 'Failed to load');
        this.loading.set(false);
      }
    });
  }
  thumbSrc = computed(():string | SafeValue => {
    const ev = this.evidence(); if (!ev) { return SafeArray;}
    else{ return this.api.thumbUrlById(ev.id, ev.thumbnailKey);}

  });

  canDelete(ev: Evidence) {
    return !ev.legalHold; // block if on legal hold
  }

  async onDelete(ev: Evidence) {
    if (!this.canDelete(ev)) return;
    const ok = confirm('Delete this evidence permanently? This cannot be undone.');
    if (!ok) return;

    this.deleting.set(true);
    this.http.delete(`${this.base}/${ev.id}`).subscribe({
      next: () => this.router.navigateByUrl(`${this.base}`),

      error: e => {
        alert(e?.error?.message || 'Delete failed');
        this.deleting.set(false);
      }
    });
  }

  dimLabel(ev: Evidence){
    if (ev.widthPx && ev.heightPx) return `${ev.widthPx} × ${ev.heightPx}px`;
    return '—';
  }

  tzLabel(mins?: number | null){
    if (mins == null) return '—';
    const sign = mins >= 0 ? '+' : '-';
    const m = Math.abs(mins);
    const hh = Math.floor(m/60).toString().padStart(2,'0');
    const mm = (m%60).toString().padStart(2,'0');
    return `UTC${sign}${hh}:${mm}`;
  }

  hasVideo(ev: Evidence){ return !!(ev.container && (ev.videoCodec || ev.durationMs)); }
}
