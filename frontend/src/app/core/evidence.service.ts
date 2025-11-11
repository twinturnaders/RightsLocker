import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';


export interface Evidence {
  id: string;
  title?: string | null;
  description?: string | null;

  capturedAt?: string | null;   // ISO
  createdAt?: string | null;    // ISO
  updatedAt?: string | null;    // ISO

  captureLatlon?: string | null;   // your format (e.g., "46.87,-113.99")
  captureAccuracyM?: number | null;

  originalKey?: string | null;
  originalSizeB?: number | null;
  originalSha256?: string | null;

  redactedKey?: string | null;
  redactedSize?: number | null;
  thumbnailKey?: string | null;

  status?: 'RECEIVED'|'PROCESSING'|'READY'|'ERROR'|string;
  legalHold?: boolean | null;

  // rich metadata
  exifDateOriginal?: string | null;   // ISO
  tzOffsetMinutes?: number | null;
  captureAltitudeM?: number | null;
  captureHeadingDeg?: number | null;

  cameraMake?: string | null;
  cameraModel?: string | null;
  lensModel?: string | null;
  software?: string | null;

  widthPx?: number | null;
  heightPx?: number | null;
  orientationDeg?: number | null;

  container?: string | null;
  videoCodec?: string | null;
  audioCodec?: string | null;
  durationMs?: number | null;
  videoFps?: number | null;
  videoRotationDeg?: number | null;
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  legalHold?: boolean;
}

export interface FinalizeResponse {
  evidence: Evidence;
  shareToken?: string | null; // present if anonymous
}

@Injectable({ providedIn: 'root' })
export class EvidenceApi {
  private http = inject(HttpClient);
  base = `${environment.apiBase}/evidence`;

  list(opts: { status?: string; page?: number; size?: number } = {}) {
    let params = new HttpParams();
    if (opts.status) params = params.set('status', opts.status);
    if (opts.page != null) params = params.set('page', String(opts.page));
    if (opts.size != null) params = params.set('size', String(opts.size));
    return this.http.get<Page<Evidence>>(this.base, { params });
  }

  get(id: string): Observable<Evidence> {
    return this.http.get<Evidence>(`/${this.base}/${id}`);
  }


  setLegalHold(id: string, legalHold: boolean) {
    return this.http.post<Evidence>(`/${this.base}/${id}/legal-hold`, { legalHold });
  }

  download(id: string, type: 'redacted'|'thumbnail'|'original' = 'redacted') {
    const params = new HttpParams().set('type', type);
    return this.http.get<{ url: string }>(`/${this.base}/${id}/download`, { params });
  }

  presignUpload(filename: string, contentType: string) {
    return this.http.post<{ key: string; url: string; headers: Record<string, string | string[]> }>(
      `/${this.base}/presign-upload`,
      { filename, contentType }
    );
  }

  finalize(payload: {
    key: string; title?: string; description?: string;
    capturedAtIso?: string; lat?: number; lon?: number; accuracy?: number;
    redactMode?: 'BLUR' | 'NONE';                // NEW
  }) {
    return this.http.post<FinalizeResponse>(`/${this.base}/finalize`, payload);
  }

  // ----- Share (public) -----
  getShare(token: string) {
    return this.http.get<{
      token: string;
      expiresAt: string;
      allowOriginal: boolean;
      evidence: {
        id: string; title?: string; description?: string;
        capturedAt?: string; status: string; hasRedacted: boolean; hasThumb: boolean;
      };
      links: { redactedUrl?: string|null; originalUrl?: string|null; thumbUrl?: string|null }
    }>(`/${environment.apiBase}/share/${token}`);
  }

  thumbUrlById(id: string){ return `/${this.base}/thumb?id=${id}`; }
  thumbUrlByKey(key: string){ return `/${this.base}/thumb?key=${encodeURIComponent(key)}`; }

}
