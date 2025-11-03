import { Component, ViewChild } from '@angular/core';
import { NgIf } from '@angular/common';
import { Evidence, EvidenceApi } from '../../../core/evidence.service';
import { EvidenceUploadComponent } from '../evidence-upload/evidence-upload.component';
import { EvidenceListComponent } from '../evidence-list/evidence-list.component';
import { EvidenceDetailComponent} from '../evidence-detail/evidence-detail.component';

@Component({
  standalone: true,
  selector: 'rl-evidence-page',
  imports: [NgIf, EvidenceUploadComponent, EvidenceListComponent, EvidenceDetailComponent],
  templateUrl: 'evidence-page.component.html',
  styleUrls: ['evidence-page.component.css']
})
export class EvidencePageComponent {
  @ViewChild(EvidenceListComponent) list?: EvidenceListComponent;

  selectedId?: string;

  onUploaded(ev: Evidence) {
    // refresh list and select the new item
    this.list?.reload();
    this.selectedId = ev.id;
  }

  onPicked(ev: Evidence) {
    this.selectedId = ev.id;
  }
}
