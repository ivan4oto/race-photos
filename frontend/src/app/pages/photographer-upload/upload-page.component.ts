import {Component, computed, input, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DEFAULT_UPLOAD_CONFIG } from '../../shared/upload.config';
import { UploadItem } from '../../shared/upload.types';
import { S3UploadService } from '../../shared/s3-upload.service';
import { ActivatedRoute } from '@angular/router';
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-upload-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  styleUrls: ['./upload-page.component.css'],
  templateUrl: './upload-page.component.html'
})
export class UploadPageComponent {
  config = DEFAULT_UPLOAD_CONFIG;

  items = signal<UploadItem[]>([]);
  folderName = signal('');

  // Derived counts
  successCount = computed(() => this.items().filter(i => i.status === 'success').length);
  errorCount = computed(() => this.items().filter(i => i.status === 'error').length);
  skippedCount = computed(() => this.items().filter(i => i.status === 'skipped').length);
  hasQueued = computed(() => this.items().some(i => i.status === 'queued'));
  overallProgress = computed(() => {
    const all = this.items();
    if (all.length === 0) return 0;
    const portion = all.reduce((acc, item) => {
      if (['success', 'error', 'skipped'].includes(item.status)) {
        return acc + 1;
      }
      if (item.status === 'uploading') {
        return acc + (item.progress ?? 0) / 100;
      }
      return acc;
    }, 0);
    return Math.round((portion / all.length) * 100);
  });

  isUploading = signal(false);
  pageError = signal<string | null>(null);
  readonly eventSlug: string | null;

  constructor(
    private s3: S3UploadService,
    route: ActivatedRoute
  ) {
    this.eventSlug = route.snapshot.paramMap.get('eventSlug');
    if (!this.eventSlug) {
      this.pageError.set('Missing event slug in the URL.');
    }
  }

  onPickFiles(files: FileList | null) {
    if (!files) return;
    const next: UploadItem[] = [];
    Array.from(files).forEach(file => {
      const { acceptedMimeTypes, maxFileSizeBytes } = this.config;
      const name = file.name;
      const size = file.size;
      const allowed = acceptedMimeTypes.includes(file.type) || acceptedMimeTypes.length === 0;
      const withinSize = size <= maxFileSizeBytes;

      if (!allowed) {
        next.push({ file, key: name, name, size, status: 'skipped', progress: 0, skippedReason: `Disallowed type: ${file.type || 'unknown'}` });
      } else if (!withinSize) {
        next.push({ file, key: name, name, size, status: 'skipped', progress: 0, skippedReason: `Too large: ${(size/1024/1024).toFixed(1)} MB` });
      } else {
        next.push({ file, key: name, name, size, status: 'queued', progress: 0 });
      }
    });
    this.items.update(arr => [...arr, ...next]);
  }

  private emitItems() {
    // Shallow copy to trigger signal change detection after in-place mutations.
    this.items.set([...this.items()]);
  }

  async startUpload() {
    if (!this.eventSlug) {
      this.items.update(arr => arr.map(i => ({ ...i, status: 'error', errorMessage: 'Missing event slug' })));
      return;
    }
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
        this.emitItems();
        const names = batch.map(b => b.name);
        try {
          const folder = this.folderName().trim() || undefined;
          const presignedDtos = await this.s3.presign(names, this.eventSlug, folder);
          presignedDtos.forEach((dto) => urlMap.set(dto.name, dto.url));
        } catch (e: any) {
          batch.forEach(item => {
            item.status = 'error';
            item.errorMessage = 'Presign failed';
          });
          this.emitItems();
        }
      }

      // Upload sequentially (no concurrency) for items that have URLs
      for (const item of this.items()) {
        if (item.status !== 'presigning' && item.status !== 'queued') continue;
        const url = urlMap.get(item.name);
        if (!url) {
          item.status = 'error';
          item.errorMessage = 'Missing presigned URL';
          this.emitItems();
          continue;
        }
        item.status = 'uploading';
        item.progress = 0;
        this.emitItems();
        try {
          await this.s3.uploadWithProgress(url, item.file, pct => {
            const rounded = Math.round(pct);
            if (rounded !== item.progress) {
              item.progress = rounded;
              this.emitItems();
            }
          });
          item.status = 'success';
          item.progress = 100;
        } catch (err: any) {
          item.status = 'error';
          item.errorMessage = err?.message || 'Upload failed';
        }
        this.emitItems();
      }
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

}
