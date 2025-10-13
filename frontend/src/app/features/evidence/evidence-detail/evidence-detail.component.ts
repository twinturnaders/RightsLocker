import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AsyncPipe, DatePipe, NgIf } from '@angular/common';
import {Evidence, EvidenceApi} from '../../../core/evidence.service';

@Component({
  standalone: true,
  selector: 'rl-evidence-detail',
  imports: [NgIf, DatePipe],
  templateUrl: './evidence-detail.component.html',

})
export class EvidenceDetailComponent {
  route = inject(ActivatedRoute);
  api = inject(EvidenceApi);
  e?: Evidence;
//thumbById(id: string){ return `/api/evidence/thumb?id=${encodeURIComponent(id)}`; }
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
