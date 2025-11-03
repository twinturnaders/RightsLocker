import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import {environment} from '../../environments/environment';

export interface Evidence {
  id: string;
  title?: string;
  description?: string;
  capturedAt?: string;
  status: 'RECEIVED'|'PROCESSING'|'READY'|'ERROR'|'REDACTED';
  legalHold: boolean;
  derivativeUrl?: string;
  thumbnailUrl?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page (0-based)
  size: number;     // page size
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

  get(id: string) { if(id == null){return null}
    else{return this.http.get<Evidence>(`${this.base}/${id}`);} }

  setLegalHold(id: string, legalHold: boolean) {
    return this.http.post<Evidence>(`${this.base}/${id}/legal-hold`, { legalHold });
  }

  download(id: string, type: 'redacted'|'thumbnail'|'original' = 'redacted') {
    const params = new HttpParams().set('type', type);
    return this.http.get<{ url: string }>(`${this.base}/${id}/download`, { params });
  }

  // presign + finalize (matches your
  presignUpload(filename: string, contentType: string) {
    return this.http.post<{ key: string; url: string; headers: Record<string, string | string[]> }>(
      `${this.base}/presign-upload`,
      { filename, contentType }
    );
  }

  finalize(payload: {
    key: string; title?: string; description?: string;
    capturedAtIso?: string; lat?: number; lon?: number; accuracy?: number;
  }) {
    return this.http.post<Evidence>(`${this.base}/finalize`, payload);
  }
}
