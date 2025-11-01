import { Component, inject } from '@angular/core';
import {EvidenceApi, Evidence, Page} from '../../../core/evidence.service';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';


@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, RouterLink, DatePipe],
  templateUrl: 'evidence-list.component.html',
  styleUrls: ['evidence-list.component.css']
})
export class EvidenceListComponent {
  api = inject(EvidenceApi);

  page?: Page<Evidence>;
  loading = false;
  error = '';

  ngOnInit() { this.load(0, 20); }

  load(page: number, size: number) {
    this.loading = true; this.error = '';
    this.api.list({ page, size }).subscribe({
      next: (p) => { this.page = p; this.loading = false; },
      error: (err) => { this.error = err?.error?.message || err.statusText || 'Failed to load'; this.loading = false; }
    });
  }

  next() {
    if (!this.page) return;
    if (this.page.number + 1 < this.page.totalPages) this.load(this.page.number + 1, this.page.size);
  }
  prev() {
    if (!this.page) return;
    if (this.page.number > 0) this.load(this.page.number - 1, this.page.size);
  }
}
