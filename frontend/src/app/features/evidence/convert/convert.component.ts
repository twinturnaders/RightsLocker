import { Component, EventEmitter, Output, inject } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { EvidenceApi, Evidence, FinalizeResponse } from '../../../core/evidence.service';
import {environment} from '../../../../environments/environment';

@Component({
  selector: 'app-convert',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './convert.component.html',
  styleUrl: './convert.component.css'
})
export class ConvertComponent {
  private http = inject(HttpClient);
  private api = inject(EvidenceApi);
  private base = `${environment.apiBase}`;

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
      const presign = await this.api.presignUpload(this.file.name, this.file.type || 'application/octet-stream').toPromise();

      if(presign != null) {
        this.key = presign.key;

        // 2) PUT with progress
        await new Promise<void>((resolve, reject) => {
          this.http.put(presign.url, this.file, {reportProgress: true, observe: 'events' as any})
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
      }
      // 3) finalize (server computes hash, enqueues jobs)

      const fin = await this.api.finalize({
        key: this.key,
        title: this.title,
        description: this.blur ? 'Public convert (blur=on)' : 'Public convert',
        capturedAtIso: new Date().toISOString(),
        redactMode: this.blur ? 'BLUR' : 'NONE'     // <-- send it
      }).toPromise();

      this.msg = 'Uploaded! Processing…';
      if (fin != null) {
      const ev = fin.evidence;
      this.uploaded.emit(ev); // if user happens to be authed, parent can refresh list

      // If anonymous, we got a shareToken; poll /share/{token}
      if (fin.shareToken) {
        this.shareToken = fin.shareToken;
      }
        // 4) poll share endpoint for redacted readiness (link present)
        let tries = 0; let ready = false;
        while (tries++ < 30 && !ready) {
          const s = await this.api.getShare(this.shareToken).toPromise();
          if (s != null) {
            const redactedReady = !!s.links.redactedUrl || (!s.evidence.hasRedacted && !!s.links.originalUrl);
            if (redactedReady) {
              ready = true;
              break;
            }
          }
          await new Promise(r => setTimeout(r, 2000));
        }

        // 5) ready: download via public package + metadata pdf
        this.readyUrl = `${this.base}/share/${this.shareToken}/package?type=redacted&includeThumb=true`;
        this.metaPdfUrl = `${this.base}/share/${this.shareToken}/metadata.pdf`;
      } else {
        // Authed user: you could also poll your /api/evidence/{id} if you want
        // but they can always download individually from the detail pane.
        this.readyUrl = ''; // leave empty for authed flow
      }

      this.ok = true; this.msg = 'Ready!'; this.uploading = false;
    } catch (err: any) {
      this.msg = 'Upload failed: ' + (err?.message || 'unknown error');
      this.uploading = false;
    }
  }
}
