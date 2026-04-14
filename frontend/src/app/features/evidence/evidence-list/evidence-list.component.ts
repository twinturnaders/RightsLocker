import { Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { EvidenceApi, Evidence, Page } from '../../../core/evidence.service';

@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, DatePipe],
  templateUrl: 'evidence-list.component.html',
  styleUrls: ['evidence-list.component.css']
})
export class EvidenceListComponent implements OnInit {
  protected api = inject(EvidenceApi);

  @Output() selected = new EventEmitter<Evidence>();
  @Output() deleted = new EventEmitter<string>();

  evidence: Evidence[] = [];
  loading = false;
  error = '';
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;

  normalizeDate(value: string | number | null | undefined): string | number | null {
    if (value == null) return null;
    if (typeof value === 'number') return value * 1000;
    return value;
  }

  ngOnInit() {
    this.load();
  }

  reload() {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';

    this.api.list(this.currentPage, this.pageSize).subscribe({
      next: (page: Page<Evidence>) => {
        this.evidence = page.content;
        this.totalPages = page.totalPages ?? 0;
        this.loading = false;

        if (this.evidence.length === 0 && this.currentPage > 0) {
          this.currentPage = 0;
          this.load();
        }
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to load evidence';
        this.loading = false;
      }
    });
  }

  onPageChange(newPage: number) {
    if (newPage < 0 || (this.totalPages && newPage >= this.totalPages)) return;
    this.currentPage = newPage;
    this.load();
  }

  next() {
    this.onPageChange(this.currentPage + 1);
  }

  prev() {
    this.onPageChange(this.currentPage - 1);
  }

  pick(e: Evidence) {
    this.selected.emit(e);
  }

  sharePdf(ev: Evidence, event?: MouseEvent) {
    event?.stopPropagation();
    if (!ev?.id) return;

    // Use the API method that includes auth headers
    this.api.downloadPackage(ev.id, 'redacted').subscribe({
      next: (response) => {
        if (response.url) {
          window.open(response.url, '_blank', 'noopener');
        }
      },
      error: (err) => {
        console.error('Failed to get PDF URL:', err);
        this.error = 'Failed to access PDF';
      }
    });
  }

  downloadZip(ev: Evidence, event?: MouseEvent) {
    event?.stopPropagation();
    if (!ev?.id) return;

    // Use the API method that includes auth headers
    this.api.downloadPackage(ev.id, 'original').subscribe({
      next: (response) => {
        if (response.url) {
          window.open(response.url, '_blank', 'noopener');
        }
      },
      error: (err) => {
        console.error('Failed to get download URL:', err);
        this.error = 'Failed to access download';
      }
    });
  }

  delete(ev: Evidence, event?: MouseEvent) {
    event?.stopPropagation();
    if (!ev?.id) return;
    if (!confirm(`Delete "${ev.title || 'Untitled'}"? This cannot be undone.`)) return;

    this.loading = true;
    this.api.delete(ev.id).subscribe({
      next: () => {
        this.loading = false;
        this.deleted.emit(ev.id);
        this.load();
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to delete evidence';
        this.loading = false;
      }
    });
  }

  trackById(_index: number, item: Evidence) {
    return item.id;
  }
}
