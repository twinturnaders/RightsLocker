import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import {environment} from '../../../environments/environment';

export interface Evidence {
  id: string; title?: string; description?: string; capturedAt?: string;
  status: 'RECEIVED'|'PROCESSING'|'READY'|'ERROR'|'REDACTED';
  legalHold: boolean; derivativeUrl?: string; thumbnailUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class EvidenceApi {
  private http = inject(HttpClient);
  base = `${environment.apiBase}/api/evidence`;

  list(opts: {status?: string, page?: number, size?: number} = {}){
    let params = new HttpParams();
    if (opts.status) params = params.set('status', opts.status);
    if (opts.page!=null) params = params.set('page', opts.page);
    if (opts.size!=null) params = params.set('size', opts.size);
    return this.http.get<any>(this.base, { params });
  }
  get(id: string){ return this.http.get<Evidence>(`${this.base}/${id}`); }
  setLegalHold(id: string, legalHold: boolean){ return this.http.post<Evidence>(`${this.base}/${id}/legal-hold`, { legalHold }); }
  download(id: string){ return this.http.get<{url: string}>(`${this.base}/${id}/download`); }

  uploadMultipart(file: File, meta?: Record<string, any>){
    const fd = new FormData();
    fd.append('file', file);
    Object.entries(meta || {}).forEach(([k,v]) => v!=null && fd.append(k, String(v)));
    return this.http.post<Evidence>(this.base, fd);
  }

  presignUpload(key: string, contentType: string){
    const params = new HttpParams().set('key', key).set('contentType', contentType);
    return this.http.post<any>(`${this.base}/presign-upload`, null, { params });
  }
}
