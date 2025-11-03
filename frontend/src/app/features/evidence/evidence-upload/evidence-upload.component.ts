import { Component, EventEmitter, Output, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { Evidence, EvidenceApi } from '../../../core/evidence.service';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

@Component({
  standalone: true,
  selector: 'rl-evidence-upload',
  imports: [NgIf, ReactiveFormsModule, FormsModule],
  templateUrl: 'evidence-upload.component.html',
})
export class EvidenceUploadComponent {
  private api = inject(EvidenceApi);
  private router = inject(Router);
  blur:boolean = false;

  @Output() uploaded = new EventEmitter<Evidence>();

  progress = 0;
  msg = '';

  async onFile(e: Event) {
    const file = (e.target as HTMLInputElement).files?.[0];

    if (!file) return;

    this.progress = 0;
    this.msg = 'Requesting upload URL…';

    this.api.presignUpload(file.name, file.type || 'application/octet-stream')
      .subscribe({
        next: async (res) => {
          try {
            // Build signed headers from backend (may be string or string[])
            const hdrs = new Headers();
            Object.entries(res.headers || {}).forEach(([k, v]) => {
              if (Array.isArray(v)) hdrs.set(k, v.join(','));
              else if (v != null) hdrs.set(k, String(v));
            });
            if (!hdrs.has('Content-Type')) {
              hdrs.set('Content-Type', file.type || 'application/octet-stream');
            }

            this.msg = 'Uploading…';
            await this.putWithProgress(res.url, hdrs, file, p => this.progress = p);

            this.msg = 'Finalizing…';
            this.api.finalize({
              key: res.key,
              title: file.name,
              description: '',
              capturedAtIso: new Date().toISOString(),
            }).subscribe({
              next: (ev: Evidence) => {
                this.msg = 'Uploaded! Processing…';
                this.progress = 100;
                this.uploaded.emit(ev);           // tell parent to refresh/select
                // Optional: also deep-link
                 this.router.navigate(['/evidence', ev.id]);
              },
              error: (err) => {
                this.msg = 'Finalize failed: ' + (err?.error?.message || err.statusText || 'unknown');
              }
            });

          } catch (err: any) {
            this.msg = 'Upload failed: ' + (err?.message || 'unknown error');
          }
        },
        error: (err) => {
          this.msg = 'Presign failed: ' + (err?.error?.message || err.statusText || 'unknown error');
        }
      });
  }

  private putWithProgress(url: string, headers: Headers, file: File, onProgress: (p: number) => void) {
    return new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', url, true);
      headers.forEach((v, k) => xhr.setRequestHeader(k, v));
      xhr.upload.onprogress = (evt) => {
        if (evt.lengthComputable) onProgress(Math.round((evt.loaded / evt.total) * 100));
      };
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) { onProgress(100); resolve(); }
        else reject(new Error(`PUT failed: ${xhr.status} ${xhr.statusText}`));
      };
      xhr.onerror = () => reject(new Error('Network error during PUT'));
      xhr.send(file);
    });
  }


}
