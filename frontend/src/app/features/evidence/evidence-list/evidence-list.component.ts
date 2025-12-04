import {Component, EventEmitter, Output, inject, OnInit, ViewChild, AfterViewInit} from '@angular/core';
import {EvidenceApi, Evidence, Page} from '../../../core/evidence.service';
import {DatePipe, NgFor, NgIf} from '@angular/common';
import {EvidenceDetailComponent} from '../evidence-detail/evidence-detail.component';

@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, DatePipe],
  templateUrl: 'evidence-list.component.html',
  styleUrls: ['evidence-list.component.css']
})
export class EvidenceListComponent implements OnInit{

  // @ViewChild(EvidenceDetailComponent)
  // evidenceDetail!: EvidenceDetailComponent;
  protected api = inject(EvidenceApi);


  @Output() selected = new EventEmitter<Evidence>();

  evidence: Evidence[] = [];
  id: string = '';
  loading = false;
  error = '';

  // backend `Page` is almost always zero-based, so start at 0
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;

  // getDetails(e: Evidence) {
  //   this.pick(e)
  //   this.evidenceDetail.evidenceDetail(e.id);
  // }


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
    this.selected.emit((e));
  }
}
