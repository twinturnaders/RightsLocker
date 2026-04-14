import { Component, ViewChild } from '@angular/core';
import { NgIf } from '@angular/common';
import { Evidence, EvidenceApi } from '../../../core/evidence.service';

import { EvidenceListComponent } from '../evidence-list/evidence-list.component';
import { EvidenceDetailComponent } from '../evidence-detail/evidence-detail.component';
import { ConvertComponent } from '../convert/convert.component';

@Component({
  standalone: true,
  selector: 'rl-evidence-page',
  imports: [NgIf, EvidenceListComponent, EvidenceDetailComponent, ConvertComponent],
  templateUrl: 'evidence-page.component.html',
  styleUrls: ['evidence-page.component.css']
})
export class EvidencePageComponent {
  @ViewChild(EvidenceListComponent) list?: EvidenceListComponent;

  selectedId?: string;

  onUploaded(ev: Evidence) {
    this.list?.reload();
    this.selectedId = ev.id;
  }

  onPicked(ev: Evidence) {
    this.selectedId = ev.id;
  }

  onDeleted() {
    this.selectedId = undefined;
    this.list?.reload();
  }
}
