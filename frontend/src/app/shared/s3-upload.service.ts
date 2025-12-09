import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PresignedDto } from './upload.types';
import { firstValueFrom, map } from 'rxjs';
import { API_BASE_URL } from './api.config';


@Injectable({ providedIn: 'root' })
export class S3UploadService {
  constructor(private http: HttpClient, @Inject(API_BASE_URL) private apiBaseUrl: string) {}


  async presign(names: string[], eventSlug: string, folderName?: string): Promise<PresignedDto[]> {
    const url = `${this.apiBaseUrl}/s3/events/${eventSlug}/presigned-urls`;
    return await firstValueFrom(
      this.http.post<{ urls: PresignedDto[] }>(url, { names, folderName }).pipe(map(res => res.urls || []))
    );
  }

  uploadWithProgress(presignedUrl: string, file: File, onProgress: (pct: number) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', presignedUrl, true);
      // Intentionally do not set Content-Type or any headers

      xhr.upload.onprogress = (evt) => {
        if (evt.lengthComputable) {
          const pct = Math.floor((evt.loaded / evt.total) * 100);
          onProgress(pct);
        }
      };

      xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
          if (xhr.status >= 200 && xhr.status < 300) {
            onProgress(100);
            resolve();
          } else {
            reject(new Error(`Upload failed: HTTP ${xhr.status}`));
          }
        }
      };

      xhr.onerror = () => reject(new Error('Network error during upload'));
      xhr.onabort = () => reject(new Error('Upload aborted'));

      xhr.send(file);
    });
  }
}
