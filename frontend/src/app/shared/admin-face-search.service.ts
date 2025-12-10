import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface AdminFaceSearchMatch {
  url: string;
  faceId: string;
  similarity: number;
  confidence?: number | null;
}

export interface AdminFaceSearchResponse {
  eventId: string;
  probePhotoKey: string;
  matches: AdminFaceSearchMatch[];
}

@Injectable({ providedIn: 'root' })
export class AdminFaceSearchService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  uploadProbe(eventId: string, file: File): Promise<AdminFaceSearchResponse> {
    const url = `${this.apiBaseUrl}/admin/search/events/${encodeURIComponent(eventId)}/faces`;
    const form = new FormData();
    form.append('file', file);
    return firstValueFrom(this.http.post<AdminFaceSearchResponse>(url, form));
  }
}
