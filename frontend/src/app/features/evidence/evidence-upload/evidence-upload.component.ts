import { Component, inject } from '@angular/core';
import { EvidenceApi} from '../../../core/evidence.service';
import { NgIf } from '@angular/common';

@Component({
  standalone: true,
  selector: 'rl-evidence-upload',
  imports: [NgIf],
  templateUrl: 'evidence-upload.component.html',
})
export class EvidenceUploadComponent {
  api = inject(EvidenceApi);
  progress = 0;
  msg = '';

  async onFile(e: Event) {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.progress = 0;
    this.msg = 'Requesting upload URL...';

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
            // Ensure content-type matches what was signed
            if (!hdrs.has('Content-Type')) {
              hdrs.set('Content-Type', file.type || 'application/octet-stream');
            }

            // Use XHR for progress (fetch has no upload progress)
            this.msg = 'Uploading...';
            await this.putWithProgress(res.url, hdrs, file, p => this.progress = p);

            this.msg = 'Finalizing...';
            this.api.finalize({
              key: res.key,
              title: file.name,
              description: '',
              capturedAtIso: new Date().toISOString(),
            }).subscribe({
              next: () => { this.msg = 'Uploaded! Processing…'; this.progress = 100; },
              error: (err) => { this.msg = 'Finalize failed: ' + (err?.error?.message || err.statusText); }
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
