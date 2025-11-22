import {Component, EventEmitter, Output, inject, OnInit} from '@angular/core';
import {EvidenceApi, Evidence, Page} from '../../../core/evidence.service';
import {AsyncPipe, DatePipe, NgFor, NgIf} from '@angular/common';
import {HttpClient} from '@angular/common/http';


@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, DatePipe],
  templateUrl: 'evidence-list.component.html',
  styleUrls: ['evidence-list.component.css']
})
export class EvidenceListComponent implements OnInit {
  private api = inject(EvidenceApi);

  @Output() selected = new EventEmitter<Evidence>();
  evidence: Evidence[] = [];
  page: Evidence[] = [];
  loading = false;
  error = '';
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;

  constructor(private http: HttpClient, private evidenceApi: EvidenceApi) {
  }


  ngOnInit() { this.load(); }

  reload() {
    if (!this.evidence) return this.load();
    this.load();
  }

  load() {
    this.loading = true; this.error = '';
    this.api.list( this.currentPage, this.pageSize ).subscribe((evidence: Page<Evidence>) => {
      this.evidence = evidence.content;
      this.totalPages = evidence.totalPages;
    });
  }

  onPageChange(newPage:number): void {
    this.currentPage = newPage;
    this.load();
  }

  next() {
    this.currentPage += 1;
    this.load();}
  prev() {
    this.currentPage -= 1;
    this.load();}

  pick(e: Evidence) { this.selected.emit(e); }
}

