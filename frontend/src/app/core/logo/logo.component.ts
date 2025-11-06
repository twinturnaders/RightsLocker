import { Component, Input } from '@angular/core';

type Variant = 'light' | 'dark' | 'mono';

@Component({
  standalone: true,
  selector: 'logo',
  templateUrl: './logo.component.html'
})
export class LogoComponent {
  /** CSS width value (e.g., "260px" or "100%"). Height scales via viewBox. */
  @Input() width: string = '320px';
  /** 'light' | 'dark' | 'mono' */
  @Input() variant: Variant = 'dark';
  /** show or hide the RIGHTSLocker wordmark */
  @Input() showWordmark = true;

  get palette() {
    if (this.variant === 'mono') {
      return {
        shackleA: '#6b7280', shackleB: '#9ca3af', shackleC: '#6b7280',
        bodyTop: '#9ca3af', bodyBot: '#6b7280',
        bezel: '#111827', bezelStroke: '#374151',
        lensInnerStart: '#9ca3af', lensInnerMid: '#6b7280', lensInnerDeep: '#111827',
        rights: '#111827', locker: '#111827'
      };
    }
    if (this.variant === 'light') {
      return {
        shackleA: '#8f99a4', shackleB: '#e6edf3', shackleC: '#7c8894',
        bodyTop: '#f5c9b4', bodyBot: '#c6846a',
        bezel: '#0d1116', bezelStroke: '#2a2f36',
        lensInnerStart: '#7fd3ff', lensInnerMid: '#2e7aa3', lensInnerDeep: '#01131c',
        rights: '#1e2525', locker: '#b06c58'
      };
    }
    // dark (default)
    return {
      shackleA: '#9aa3ad', shackleB: '#e5ecf2', shackleC: '#8b96a1',
      bodyTop: '#f3c3ad', bodyBot: '#c5856b',
      bezel: '#0d1116', bezelStroke: '#2a2f36',
      lensInnerStart: '#7fd3ff', lensInnerMid: '#2e7aa3', lensInnerDeep: '#01131c',
      rights: '#1e2525', locker: '#b06c58'
    };
  }
}
