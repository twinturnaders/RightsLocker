import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'bytes' })
export class BytesPipe implements PipeTransform {
  transform(v?: number | null): string {
    if (v == null) return '—';
    const units = ['B','KB','MB','GB','TB'];
    let i = 0; let n = v;
    while (n >= 1024 && i < units.length-1){ n /= 1024; i++; }
    return `${n.toFixed(n<10?2:1)} ${units[i]}`;
  }
}
