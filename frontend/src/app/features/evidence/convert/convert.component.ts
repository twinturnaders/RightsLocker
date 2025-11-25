import { Component, EventEmitter, Output, inject } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { EvidenceApi, Evidence, FinalizeResponse } from '../../../core/evidence.service';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth.service';

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
  protected auth = inject(AuthService);
  private base = `${environment.apiBase}`;

  // UI state
  file?: File;
  blur = false;
  hold = false;                        // <— bind to checkbox
  uploading = false;
  progress = 0;
  ok = false;
  msg = '';
  readyUrl = '';
  metaPdfUrl = '';
  shareToken = '';
  key = '';
  title = '';

  @Output() uploaded = new EventEmitter<Evidence>();

  pick(e: Event) {
    const f = (e.target as HTMLInputElement).files?.[0];
    if (!f) return;
    this.file = f;
    this.title = f.name;
    this.msg = '';
    this.progress = 0;
  }

  reset() {
    this.file = undefined;
    this.progress = 0;
    this.ok = false;
    this.msg = '';
    this.readyUrl = '';
    this.metaPdfUrl = '';
    this.key = '';
    this.shareToken = '';
    this.hold = false;
    this.blur = false;
  }

  async start() {
    if (!this.file) return;
    this.uploading = true;
    this.msg = '';
    this.readyUrl = '';
    this.metaPdfUrl = '';


    try {
      // 1) presign
      const presign = await this.api
        .presignUpload(this.file.name, this.file.type || 'application/octet-stream')
        .toPromise();

      if (presign) {
        this.key = presign.key;

        // 2) PUT with progress
        await new Promise<void>((resolve, reject) => {
          this.http
            .put(presign.url, this.file, { reportProgress: true, observe: 'events' as any })
            .subscribe({
              next: (ev) => {
                if ((ev as any).type === HttpEventType.UploadProgress) {
                  const p = ev as any;
                  this.progress = Math.round(100 * (p.loaded / p.total));
                }
              },
              error: reject,
              complete: () => resolve(),
            });
        });
      }

      // 3) finalize (server computes hash, enqueues jobs)
      const fin = await this.api
        .finalize({
          key: this.key,
          title: this.title,
          description: this.blur ? 'Public convert (blur=on)' : 'Public convert',
          capturedAtIso: new Date().toISOString(),
          redactMode: this.blur ? 'BLUR' : 'NONE'

        })
        .toPromise();

      this.msg = 'Uploaded! Processing…';

      if (fin) {
        const ev = fin.evidence;
        this.uploaded.emit(ev);

        // If the user is authenticated and checked "Legal Hold", set it now.
        if (this.hold && this.auth.token && ev?.id) {
          try {
            await this.api.setLegalHold(String(ev.id), true).toPromise();
          } catch {
            // non-fatal for uploader
          }
        }

        if (fin.shareToken) {
          // Anonymous flow: poll until redacted (or original if no redaction) is ready
          this.shareToken = fin.shareToken;
          let tries = 0;
          let ready = false;
          while (tries++ < 30 && !ready) {
            const s = await this.api.getShare(this.shareToken).toPromise();
            if (s) {
              const redactedReady =
                !!s.links.redactedUrl || (!s.evidence.hasRedacted && !!s.links.originalUrl);
              if (redactedReady) {
                ready = true;
                break;
              }
            }
            await new Promise((r) => setTimeout(r, 2000));
          }
          this.readyUrl = `${this.base}/share/${this.shareToken}/package?type=redacted&includeThumb=true`;
          this.metaPdfUrl = `${this.base}/share/${this.shareToken}/metadata.pdf`;
        } else {
          // Authed flow: no public package link; they can use detail page
          this.readyUrl = '';
          this.metaPdfUrl = '';
        }
      }

      this.ok = true;
      this.msg = 'Ready!';
      this.uploading = false;
    } catch (err: any) {
      this.msg = 'Upload failed: ' + (err?.message || 'unknown error');
      this.uploading = false;
    }
  }
}
