import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'durationMs' })
export class DurationMsPipe implements PipeTransform {
  transform(ms?: number | null): string {
    if (ms == null) return '—';
    const s = Math.floor(ms/1000);
    const hh = Math.floor(s/3600);
    const mm = Math.floor((s%3600)/60);
    const ss = s%60;
    return hh ? `${hh}:${mm.toString().padStart(2,'0')}:${ss.toString().padStart(2,'0')}`
      : `${mm}:${ss.toString().padStart(2,'0')}`;
  }
}
