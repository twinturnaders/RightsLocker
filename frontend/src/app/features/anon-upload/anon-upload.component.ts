import { Component, EventEmitter, Output, inject } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { EvidenceApi, Evidence } from '../../core/evidence.service';
import { environment } from '../../../environments/environment';

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

  file?: File;
  blur = false;
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
    this.blur = false;
  }

  async start() {
    if (!this.file || this.uploading) return;

    this.uploading = true;
    this.ok = false;
    this.msg = '';
    this.readyUrl = '';
    this.metaPdfUrl = '';
    this.progress = 0;

    try {
      const presign = await firstValueFrom(
        this.api.presignUpload(this.file.name, this.file.type || 'application/octet-stream')
      );

      this.key = presign.key;

      await new Promise<void>((resolve, reject) => {
        this.http.put(presign.url, this.file, { reportProgress: true, observe: 'events' }).subscribe({
          next: ev => {
            if (ev.type === HttpEventType.UploadProgress) {
              const total = ev.total ?? this.file!.size ?? 1;
              this.progress = Math.min(100, Math.round((ev.loaded / total) * 100));
            }
          },
          error: reject,
          complete: () => resolve()
        });
      });

      const fin = await firstValueFrom(
        this.api.finalize({
          key: this.key,
          title: this.title,
          description: this.blur ? 'Public convert (blur=on)' : 'Public convert',
          capturedAtIso: new Date().toISOString(),
          redactMode: this.blur ? 'BLUR' : 'NONE'
        })
      );

      const ev = fin.evidence;
      this.uploaded.emit(ev);

      if (fin.shareToken) {
        this.shareToken = fin.shareToken;
        this.readyUrl = `${this.base}/share/${this.shareToken}/package?type=redacted&includeThumb=true`;
        this.metaPdfUrl = `${this.base}/share/${this.shareToken}/metadata.pdf`;
      }

      this.ok = true;
      this.msg = 'Ready!';
    } catch (err: any) {
      this.msg = 'Upload failed: ' + (err?.message || 'unknown error');
    } finally {
      this.uploading = false;
    }
  }
}
