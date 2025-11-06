import { Component, EventEmitter, Output, inject } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import {EvidenceApi, Evidence, FinalizeResponse} from '../../core/evidence.service';
import {environment} from '../../../environments/environment';
import {finalize} from 'rxjs';

@Component({
  selector: 'anon-convert',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './anon-upload.component.html',
  styleUrl: './anon-upload.component.css'
})
export class AnonComponent {
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
    this.uploading = true;
    this.progress = 0;

    try {
      // 1) presign via your EvidenceApi (recommended)
      const presign = await this.api
        .presignUpload(this.file.name, this.file.type || 'application/octet-stream')
        .toPromise();

      // 2) PUT with progress
      await new Promise<void>((resolve, reject) => {
        this.http.put(presign!.url, this.file, { reportProgress: true, observe: 'events' as const })
          .subscribe({
            next: ev => {
              if (ev.type === HttpEventType.UploadProgress) {
                const total = ev.total ?? this.file!.size ?? 1; // fallback if total is missing
                this.progress = Math.min(100, Math.round((ev.loaded / total) * 100));
              }
            },
            error: reject,
            complete: () => { this.progress = 100; resolve(); }
          });
      });


      // 2) PUT with progress
      if(presign != null) {
        this.key = presign.key;
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


      const ev: any = await this.http.post(`${this.base}/evidence/finalize`,{
        key:this.key, title:this.title, description:'Public convert', capturedAtIso:null
      }).toPromise();

// If anonymous, backend returns shareToken (you coded this). Use it.
      if (ev?.shareToken) {
        this.readyUrl = `${this.base}/share/${ev.shareToken}/package?type=redacted`; // downloadable ZIP
        this.ok = true; this.msg = 'Ready!'; this.uploading = false;
        // Optionally show metadata PDF
        this.metaPdfUrl = `${this.base}/share/${ev.shareToken}/metadata.pdf`;
      } else {
        // Fallback for authed flow
        this.readyUrl = `${this.base}/evidence/${ev.id}/package?type=redacted&includeThumb=true`;
      }
      const fin = await this.api.finalize({
        key: this.key,
        title: this.title,
        description: this.blur ? 'Public convert (blur=on)' : 'Public convert',
        capturedAtIso: new Date().toISOString(),
        redactMode: this.blur ? 'BLUR' : 'NONE'     // <-- send it
      }).toPromise();

      this.ok = true; this.msg = 'Ready!'; this.uploading = false;
    } catch (err: any) {
      this.msg = 'Upload failed: ' + (err?.message || 'unknown error');
      this.uploading = false;
    }

  } catch (e: any) {
    this.uploading = false;
    throw e;
  }


}
