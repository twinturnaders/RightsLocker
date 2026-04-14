import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Evidence {
  id: string;
  title?: string | null;
  description?: string | null;

  capturedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;

  captureLatlon?: string | null;
  captureAccuracyM?: number | null;

  originalKey?: string | null;
  originalSizeB?: number | null;
  originalSha256?: string | null;

  redactedKey?: string | null;
  redactedSize?: number | null;
  thumbnailKey?: string | null;

  status?: 'RECEIVED' | 'PROCESSING' | 'READY' | 'ERROR' | string;
  legalHold?: boolean | null;

  exifDateOriginal?: string | number | null;
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

  provenanceStatus?: string | null;
  metadataIntegrity?: string | null;
  syntheticMediaRisk?: string | null;
  manipulationSignals?: string | null;
  assessmentSummary?: string | null;

  pdfUrl?: string | null;
  zipUrl?: string | null;
  shareUrl?: string | null;
  thumbnailUrl?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  legalHold?: boolean;
}

export interface PresignResponse {
  url: string;
  key: string;
}

export interface FinalizeRequest {
  key: string;
  title: string;
  description?: string;
  capturedAtIso?: string;
  redactMode?: 'NONE' | 'BLUR';
}

export interface FinalizeResponse {
  evidence: Evidence;
  shareToken?: string | null;
}

@Injectable({ providedIn: 'root' })
export class EvidenceApi {
  private http = inject(HttpClient);
  base = `${environment.apiBase}/evidence`;

  list(page: number, pageSize: number) {
    const params = new HttpParams()
      .set('page', page)
      .set('size', pageSize);

    return this.http.get<Page<Evidence>>(this.base, { params }).pipe(
      map(pageResult => ({
        ...pageResult,
        content: pageResult.content.map(ev => this.enrichEvidence(ev))
      }))
    );
  }

  get(id: string): Observable<Evidence> {
    return this.http.get<Evidence>(`${this.base}/${id}`).pipe(
      map(ev => this.enrichEvidence(ev))
    );
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  presignUpload(filename: string, contentType: string): Observable<PresignResponse> {
    return this.http.post<PresignResponse>(`${this.base}/presign-upload`, {
      filename,
      contentType
    });
  }

  finalize(req: FinalizeRequest): Observable<FinalizeResponse> {
    return this.http.post<FinalizeResponse>(`${this.base}/finalize`, req);
  }

  setLegalHold(id: string, legalHold: boolean) {
    return this.http.post<Evidence>(`${this.base}/${id}/${legalHold}`, {});
  }

  downloadPackage(id: string, type: 'original' | 'redacted' = 'original') {
    const params = new HttpParams().set('type', type);
    return this.http.get<{ url: string }>(`${this.base}/${id}/download`, { params });
  }

  downloadPdf(id: string) {
    return this.http.get<{ url: string }>(`${this.base}/${id}/pdf-url`);
  }

  getPdfUrl(id: string): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/${id}/pdf-url`);
  }

  thumbUrlById(id?: string | null, key?: string | null): string | null {
    if (!id || !key) return null;
    const params = new HttpParams()
      .set('id', id)
      .set('key', key);
    return `${this.base}/thumb?${params.toString()}`;
  }

  sharePdfUrlById(id: string): string {
    return `${this.base}/${id}/pdf`;
  }

  private enrichEvidence(ev: Evidence): Evidence {
    return {
      ...ev,
      thumbnailUrl: ev.thumbnailUrl || this.thumbUrlById(ev.id, ev.thumbnailKey),
      zipUrl: ev.zipUrl || `${this.base}/${ev.id}/download?type=original`,
      pdfUrl: ev.pdfUrl || `${this.base}/${ev.id}/pdf`
    };
  }
}
