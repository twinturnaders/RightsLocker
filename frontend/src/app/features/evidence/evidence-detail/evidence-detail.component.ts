import { Component, EventEmitter, Input, Output, effect, inject, signal } from '@angular/core';
import { DatePipe, NgIf, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Evidence, EvidenceApi } from '../../../core/evidence.service';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, RouterLink, NgOptimizedImage, DatePipe],
  templateUrl: 'evidence-detail.component.html',
  styleUrls: ['evidence-detail.component.css']
})
export class EvidenceDetailComponent {
  protected api = inject(EvidenceApi);

  @Input() id?: string;
  @Output() deleted = new EventEmitter<string>();

  evidence = signal<Evidence | null>(null);
  loading = signal(false);
  error = signal('');
  deleting = signal(false);

  constructor() {
    effect(() => {
      const evidenceId = this.id;
      if (!evidenceId) {
        this.evidence.set(null);
        return;
      }
      this.load(evidenceId);
    });
  }

  load(id: string) {
    this.loading.set(true);
    this.error.set('');

    this.api.get(id).subscribe({
      next: ev => {
        this.evidence.set(ev);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err?.error?.message || 'Failed to load evidence');
        this.loading.set(false);
      }
    });
  }

  thumbSrc() {
    const ev = this.evidence();
    return ev?.thumbnailUrl || (ev?.thumbnailKey ? this.api.thumbUrlById(ev.id, ev.thumbnailKey) : null);
  }

  canDelete(ev: Evidence) {
    return !ev.legalHold;
  }

  openPdf(ev: Evidence) {
    window.open(ev.pdfUrl || this.api.sharePdfUrlById(ev.id), '_blank', 'noopener');
  }

  openZip(ev: Evidence) {
    window.open(ev.zipUrl || `${this.api.base}/${ev.id}/download?type=original`, '_blank', 'noopener');
  }

  onDelete(ev: Evidence) {
    if (!this.canDelete(ev)) return;
    if (!confirm(`Delete "${ev.title || 'Untitled'}"? This cannot be undone.`)) return;

    this.deleting.set(true);
    this.api.delete(ev.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleted.emit(ev.id);
        this.evidence.set(null);
      },
      error: err => {
        this.error.set(err?.error?.message || 'Failed to delete evidence');
        this.deleting.set(false);
      }
    });
  }

  tzLabel(minutes: number | null | undefined): string {
    if (minutes == null) return '—';
    const hours = minutes / 60;
    return hours >= 0 ? `UTC+${hours}` : `UTC${hours}`;
  }

  dimLabel(ev: Evidence): string {
    if (ev.widthPx == null && ev.heightPx == null) return '—';
    return `${ev.widthPx ?? '—'} × ${ev.heightPx ?? '—'} px`;
  }

  hasVideo(ev: Evidence): boolean {
    return !!(ev.container || ev.videoCodec || ev.audioCodec || ev.durationMs != null || ev.videoFps != null || ev.videoRotationDeg != null);
  }
}
