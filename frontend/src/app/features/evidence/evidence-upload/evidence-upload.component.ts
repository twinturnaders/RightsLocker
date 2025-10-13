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
  progress = 0; msg = '';

  async onFile(e: Event){
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    // Option A: Direct multipart to backend (simple path)
    this.api.uploadMultipart(file, { title: file.name }).subscribe({
      next: () => this.msg = 'Uploaded! Processing…',
      error: (err) => this.msg = 'Upload failed: ' + (err?.error?.message || err.statusText)
    });

    // Option B (commented): Presigned PUT straight to S3/Spaces
    // const key = `${crypto.randomUUID()}/original-${file.name}`;
    // this.api.presignUpload(key, file.type || 'application/octet-stream').subscribe(async res => {
    //   const put = await fetch(res.url, { method: 'PUT', headers: res.headers, body: file });
    //   if (!put.ok) { this.msg = 'Upload failed'; return; }
    //   // TODO: call finalize endpoint to create Evidence row from S3 object
    //   this.msg = 'Uploaded via presign!';
    // });
  }
}
