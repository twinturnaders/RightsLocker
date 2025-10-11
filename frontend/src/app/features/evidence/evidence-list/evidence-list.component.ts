import { Component, inject } from '@angular/core';
import { EvidenceApi} from '../../../core/evidence.service/evidence.service.component';
import {CommonModule, DatePipe, DecimalPipe, NgFor, NgIf} from '@angular/common';
import { RouterLink } from '@angular/router';
import { StatusChipComponent} from '../../../ui/status-chip/status-chip.component';


@Component({
  standalone: true,
  selector: 'rl-evidence-list',
  imports: [NgFor, NgIf, RouterLink, DatePipe, StatusChipComponent, DecimalPipe, CommonModule],
  templateUrl:'./evidence-list.component.html',
  styleUrls: ['./evidence-list.component.css']
})
export class EvidenceListComponent {
  api = inject(EvidenceApi); page:any;
  ngOnInit(){ this.api.list({ page:0, size:50 }).subscribe(p=> this.page = p); }
  thumbById(id: string){ return `/api/evidence/thumb?id=${encodeURIComponent(id)}`; }

}
