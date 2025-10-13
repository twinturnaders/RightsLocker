import { Component, inject } from '@angular/core';
import { EvidenceApi, Evidence} from '../../../core/evidence.service';
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
  items: any;
  ngOnInit(){ this.api.list({ page: 0, size: 20 }).subscribe(p => this.items = p); }
}
