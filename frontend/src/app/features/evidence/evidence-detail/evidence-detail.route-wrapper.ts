import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { AsyncPipe, NgIf } from '@angular/common';
import { EvidenceDetailComponent } from './evidence-detail.component';

@Component({
  standalone: true,
  imports: [NgIf, AsyncPipe, EvidenceDetailComponent],
  template: `
    <ng-container *ngIf="id$ | async as id; else bad">
      <rl-evidence-detail [id]="id"></rl-evidence-detail>
    </ng-container>
    <ng-template #bad>
      <div class="card">Invalid or missing ID.</div>
    </ng-template>
  `
})
export class EvidenceDetailRouteWrapper {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  // If your route is /evidence/:id
  id$ = this.route.paramMap.pipe(
    map(pm => pm.get('id')),
  );
}
