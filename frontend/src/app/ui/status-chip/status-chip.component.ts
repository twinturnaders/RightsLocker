import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';


@Component({
  standalone: true,
  selector: 'rl-status-chip',
  imports: [
    NgClass
  ],
  template: `<span class="badge" [ngClass]="'status-' + value">{{ value }}</span>`
})
export class StatusChipComponent { @Input() value: 'RECEIVED'|'PROCESSING'|'READY'|'REDACTED'|'ERROR' = 'RECEIVED'; }
