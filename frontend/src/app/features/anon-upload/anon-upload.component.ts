import { Component, EventEmitter, Output, inject } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { EvidenceApi, Evidence} from '../../core/evidence.service';

@Component({
  selector: 'anon-convert',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './convert.component.html',
  styleUrl: './convert.component.css'
})
export class AnonComponent {
  private http = inject(HttpClient);
  private api = inject(EvidenceApi);

  file?: File; blur = false; uploading = false; progress = 0; ok = false;
  msg = ''; readyUrl = ''; metaPdfUrl = ''; shareToken = ''; key = ''; title = '';
  @Output() uploaded = new EventEmitter<Evidence>(); // still emits when authed flows use this

  pick(e: Event) {
    const f = (e.target as HTMLInputElement).files?.[0];
    if (!f) return;
    this.file = f; this.title = f.name; this.msg = ''; this.progress = 0;
  }

  reset() {
    this.file = undefined; this.progress = 0; this.ok = false; this.msg = '';
    this.readyUrl = ''; this.metaPdfUrl = ''; this.key = ''; this.shareToken = '';
  }

  async start() {
    if (!this.file) return;
    this.uploading = true; this.msg = ''; this.readyUrl = ''; this.metaPdfUrl = '';

    try {
      // 1) presign
      const presign: any = await this.http.post(
        '/api/evidence/presign-upload',
        { filename: this.file.name, contentType: this.file.type || 'application/octet-stream' }
      ).toPromise();
      this.key = presign.key;

      // 2) PUT with progress
      await new Promise<void>((resolve, reject) => {
        this.http.put(presign.url, this.file, { reportProgress: true, observe: 'events' as any })
          .subscribe({
            next: ev => {
              if ((ev as any).type === HttpEventType.UploadProgress) {
                const p = ev as any;
                this.progress = Math.round(100 * (p.loaded / p.total));
              }
            },
            error: reject,
            complete: () => resolve()
          });
      });

      // 3) finalize (server computes hash, enqueues jobs)

      const ev: any = await this.http.post('/api/evidence/finalize',{
        key:this.key, title:this.title, description:'Public convert', capturedAtIso:null
      }).toPromise();

// If anonymous, backend returns shareToken (you coded this). Use it.
      if (ev?.shareToken) {
        this.readyUrl = `/api/share/${ev.shareToken}/package?type=redacted`; // downloadable ZIP
        this.ok = true; this.msg = 'Ready!'; this.uploading = false;
        // Optionally show metadata PDF
        this.metaPdfUrl = `/api/share/${ev.shareToken}/metadata.pdf`;
      } else {
        // Fallback for authed flow
        this.readyUrl = `/api/evidence/${ev.id}/package?type=redacted&includeThumb=true`;
      }


      this.ok = true; this.msg = 'Ready!'; this.uploading = false;
    } catch (err: any) {
      this.msg = 'Upload failed: ' + (err?.message || 'unknown error');
      this.uploading = false;
    }
  }
}
