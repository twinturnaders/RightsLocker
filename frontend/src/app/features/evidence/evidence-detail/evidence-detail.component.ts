import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {AsyncPipe, CommonModule, DatePipe, NgIf} from '@angular/common';
import {Evidence, EvidenceApi} from '../../../core/evidence.service/evidence.service.component';
import {StatusChipComponent} from '../../../ui/status-chip/status-chip.component';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, DatePipe, StatusChipComponent, CommonModule, StatusChipComponent],
  templateUrl: './evidence-detail.component.html',
  styleUrls: ['./evidence-detail.component.css']
})
export class EvidenceDetailComponent {
  route = inject(ActivatedRoute);
  api = inject(EvidenceApi);
  e?: Evidence;

  ngOnInit(){
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get(id).subscribe(ev => this.e = ev);
  }

  toggleHold(evt: Event){
    if (!this.e) return; const checked = (evt.target as HTMLInputElement).checked;
    this.api.setLegalHold(this.e.id, checked).subscribe(ev => this.e = ev);
  }

  download(){ if (!this.e) return; this.api.download(this.e.id).subscribe(({url}) => window.open(url, '_blank')); }
}
