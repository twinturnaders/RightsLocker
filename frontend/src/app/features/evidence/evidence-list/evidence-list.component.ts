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

  evidence: Evidence[] = [];
  id = '';
  loading = false;
  error = '';
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;

  normalizeDate(value: string | number | null | undefined): string | number | null {
    if (value == null) return null;

    if (typeof value === 'number') {
      // Backend sends epoch seconds; Angular DatePipe expects milliseconds.
      return value * 1000;
    }

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
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to load evidence';
        this.loading = false;
      }
    });
  }

  onPageChange(newPage: number) {
    if (newPage < 0 || (this.totalPages && newPage >= this.totalPages)) {
      return;
    }
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
}
