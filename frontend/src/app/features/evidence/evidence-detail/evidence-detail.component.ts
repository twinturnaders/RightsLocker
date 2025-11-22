import { Component, inject, signal, computed } from '@angular/core';
import { AsyncPipe, DatePipe, NgIf, NgFor, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EvidenceApi, Evidence } from '../../../core/evidence.service';
import { switchMap } from 'rxjs/operators';
import { BytesPipe } from '../../../core/pipes/bytes.pipe';
import { DurationMsPipe } from '../../../core/pipes/duration.pipe';
import { environment } from '../../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/auth.service';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, RouterLink, DatePipe, BytesPipe, DurationMsPipe, DecimalPipe],
  templateUrl: './evidence-detail.component.html',
  styleUrls: ['./evidence-detail.component.css']
})
export class EvidenceDetailComponent {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(EvidenceApi);


  evidence = signal<Evidence | null>(null);
  loading = signal(true);
  deleting = signal(false);
  error = signal<string>('');

  ngOnInit() {
    this.route.paramMap.pipe(
      switchMap(p => this.api.get(p.get('id')!))
    ).subscribe({
      next: ev => { this.evidence.set(ev); this.loading.set(false); },
      error: err => { this.error.set(err?.error?.message || 'Failed to load'); this.loading.set(false); }
    });
  }

  thumbSrc = computed(() => {
    const ev = this.evidence();
    if (!ev) return null;
    if (ev.thumbnailKey) return
    return this.api.thumbUrlById(ev.id, ev.thumbnailKey);
  });

  canDelete(ev: Evidence) {
    return !ev.legalHold; // block if on legal hold
  }

  async onDelete(ev: Evidence) {
    if (!this.canDelete(ev)) return;
    const ok = confirm('Delete this evidence permanently? This cannot be undone.');
    if (!ok) return;

    this.deleting.set(true);
    this.http.delete(`${environment.apiBase}/evidence/${ev.id}`, {
      headers: this.auth.token ? { Authorization: `Bearer ${this.auth.token}` } : {}
    }).subscribe({
      next: () => this.router.navigateByUrl('/evidence'),
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
