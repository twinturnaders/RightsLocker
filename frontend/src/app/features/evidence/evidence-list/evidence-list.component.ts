import {Component, EventEmitter, Output, inject} from '@angular/core';
import {EvidenceApi, Evidence, Page} from '../../../core/evidence.service';
import { DatePipe, NgFor, NgIf } from '@angular/common';

@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, DatePipe],
  templateUrl: 'evidence-list.component.html',
  styleUrls: ['evidence-list.component.css']
})
export class EvidenceListComponent {
  private api = inject(EvidenceApi);

  @Output() selected = new EventEmitter<Evidence>();

  page?: Page<Evidence>;
  loading = false;
  error = '';

  ngOnInit() { this.load(0, 20); }

  reload() {
    if (!this.page) return this.load(0, 20);
    this.load(this.page.number, this.page.size);
  }

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

  pick(e: Evidence) { this.selected.emit(e); }
}
