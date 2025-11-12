import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DEFAULT_UPLOAD_CONFIG } from '../../shared/upload.config';
import { UploadItem } from '../../shared/upload.types';
import { S3UploadService } from '../../shared/s3-upload.service';

@Component({
  selector: 'app-upload-page',
  standalone: true,
  imports: [CommonModule],
  styleUrls: ['./upload-page.component.css'],
  templateUrl: './upload-page.component.html'
})
export class UploadPageComponent {
  config = DEFAULT_UPLOAD_CONFIG;

  items = signal<UploadItem[]>([]);

  // Derived counts
  successCount = computed(() => this.items().filter(i => i.status === 'success').length);
  errorCount = computed(() => this.items().filter(i => i.status === 'error').length);
  skippedCount = computed(() => this.items().filter(i => i.status === 'skipped').length);
  hasQueued = computed(() => this.items().some(i => i.status === 'queued'));

  isUploading = signal(false);

  constructor(private s3: S3UploadService) {}

  onPickFiles(files: FileList | null) {
    if (!files) return;
    const next: UploadItem[] = [];
    Array.from(files).forEach(file => {
      const { acceptedMimeTypes, maxFileSizeBytes } = this.config;
      const name = file.name;
      const size = file.size;
      const allowed = acceptedMimeTypes.includes(file.type) || acceptedMimeTypes.length === 0;
      const withinSize = size <= maxFileSizeBytes;

      const key = this.generateKey(name);

      if (!allowed) {
        next.push({ file, key, name, size, status: 'skipped', progress: 0, skippedReason: `Disallowed type: ${file.type || 'unknown'}` });
      } else if (!withinSize) {
        next.push({ file, key, name, size, status: 'skipped', progress: 0, skippedReason: `Too large: ${(size/1024/1024).toFixed(1)} MB` });
      } else {
        next.push({ file, key, name, size, status: 'queued', progress: 0 });
      }
    });
    this.items.update(arr => [...arr, ...next]);
  }

  async startUpload() {
    if (this.isUploading()) return;
    this.isUploading.set(true);
    try {
      // Collect keys for queued items only
      const queued = this.items().filter(i => i.status === 'queued');
      // Presign in batches
      const batchSize = this.config.presignBatchSize;
      const urlMap = new Map<string, string>();
      for (let i = 0; i < queued.length; i += batchSize) {
        const batch = queued.slice(i, i + batchSize);
        batch.forEach(item => item.status = 'presigning');
        const names = batch.map(b => b.key);
        try {
          const presignedDtos = await this.s3.presign(names);
          presignedDtos.forEach((dto) => urlMap.set(dto.name, dto.url));
        } catch (e: any) {
          batch.forEach(item => {
            item.status = 'error';
            item.errorMessage = 'Presign failed';
          });
        }
      }

      // Upload sequentially (no concurrency) for items that have URLs
      for (const item of this.items()) {
        if (item.status !== 'presigning' && item.status !== 'queued') continue;
        const url = urlMap.get(item.key);
        if (!url) {
          item.status = 'error';
          item.errorMessage = 'Missing presigned URL';
          continue;
        }
        item.status = 'uploading';
        item.progress = 0;
        try {
          await this.s3.uploadWithProgress(url, item.file, pct => {
            item.progress = pct;
          });
          item.status = 'success';
          item.progress = 100;
        } catch (err: any) {
          item.status = 'error';
          item.errorMessage = err?.message || 'Upload failed';
        }
      }
      // trigger signal change
      this.items.set([...this.items()]);
    } finally {
      this.isUploading.set(false);
    }
  }

  clearCompleted() {
    this.items.update(arr => arr.filter(i => i.status !== 'success'));
  }

  retryFailed() {
    this.items.update(arr => arr.map(i => (i.status === 'error' ? { ...i, status: 'queued', progress: 0, errorMessage: undefined } : i)));
    this.startUpload();
  }

  private generateKey(originalName: string): string {
    // TODO: generate date based on file/photo metadata
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const uuid = (globalThis.crypto?.randomUUID?.() ?? Math.random().toString(36).slice(2));
    const safeOriginal = originalName
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9._-]/g, '');
    return `in/2/johndoe/raw/${safeOriginal}`;
  }
}
