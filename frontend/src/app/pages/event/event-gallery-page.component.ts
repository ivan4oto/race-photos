import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { API_BASE_URL } from '../../shared/api.config';
import { finalize, Subscription } from 'rxjs';
import { zipSync } from 'fflate';

type SelfieSearchMatch = { url: string; similarity: number };
type SelfieSearchResponse = { eventId: string; probePhotoKey: string; matches: SelfieSearchMatch[] };

@Component({
  selector: 'app-event-gallery-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './event-gallery-page.component.html',
  styleUrls: ['./event-gallery-page.component.css']
})
export class EventGalleryPageComponent implements OnInit, OnDestroy {
  protected readonly eventId = signal<string | null>(null);
  protected readonly matches = signal<SelfieSearchMatch[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly missingSelfie = signal(false);
  protected readonly selectedImage = signal<string | null>(null);
  protected readonly uploading = signal(false);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly uploadError = signal<string | null>(null);
  protected readonly downloading = signal(false);

  protected readonly hasMatches = computed(() => this.matches().length > 0);
  protected readonly canSubmitSelfie = computed(() => !!this.selectedFile() && !this.uploading());

  private sub?: Subscription;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  ngOnInit(): void {
    this.sub = this.route.paramMap.subscribe(params => {
      const id = params.get('eventId');
      this.eventId.set(id);
      this.fetchMatches();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  fetchMatches(): void {
    const id = this.eventId();
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.missingSelfie.set(false);
    this.matches.set([]);
    this.http.get<SelfieSearchResponse>(`${this.apiBaseUrl}/${encodeURIComponent(id)}/search`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: res => {
          this.matches.set(res.matches || []);
          if (!res.matches || res.matches.length === 0) {
            this.error.set('No matches');
          }
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 400) {
            this.missingSelfie.set(true);
            this.error.set('Upload a selfie to start finding your photos.');
            return;
          }
          if (err.status === 403) {
            this.error.set('You do not have access to this event.');
            return;
          }
          this.error.set(err.error?.message || 'Failed to fetch matches. Try again.');
        }
      });
  }

  openImage(url: string): void {
    this.selectedImage.set(url);
  }

  closeModal(): void {
    this.selectedImage.set(null);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
        this.uploadError.set('Only image files are allowed.');
        this.selectedFile.set(null);
        return;
    }
    if (file.size > 4 * 1024 * 1024) {
      this.uploadError.set('Max file size is 4MB.');
      this.selectedFile.set(null);
      return;
    }
    this.uploadError.set(null);
    this.selectedFile.set(file);
  }

  submitSelfie(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.uploading.set(true);
    this.uploadError.set(null);
    const formData = new FormData();
    formData.append('file', file);
    this.http.post<void>(`${this.apiBaseUrl}/selfie`, formData, { observe: 'response' })
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: res => {
          if (res.status === 201 || res.status === 200) {
            this.selectedFile.set(null);
            this.fetchMatches();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.uploadError.set(err.error?.message || 'Failed to upload selfie.');
        }
      });
  }

  downloadAll(): void {
    const matches = this.matches();
    if (!matches.length || this.downloading()) return;
    this.downloading.set(true);
    this.error.set(null);
    (async () => {
      try {
        const files: Record<string, Uint8Array> = {};
        for (const match of matches) {
          const resp = await fetch(match.url);
          if (!resp.ok) {
            throw new Error(`Failed to fetch ${this.filenameFromUrl(match.url)}`);
          }
          const buffer = new Uint8Array(await resp.arrayBuffer());
          const baseName = this.filenameFromUrl(match.url);
          let name = baseName;
          let counter = 1;
          while (files[name]) {
            const dot = baseName.lastIndexOf('.');
            const stem = dot > 0 ? baseName.substring(0, dot) : baseName;
            const ext = dot > 0 ? baseName.substring(dot) : '';
            name = `${stem}-${counter}${ext}`;
            counter++;
          }
          files[name] = buffer;
        }
        const zipped = zipSync(files, { level: 6 });
        const blob = new Blob([zipped], { type: 'application/zip' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'photos.zip';
        a.click();
        URL.revokeObjectURL(url);
      } catch (err: any) {
        this.error.set(err?.message || 'Failed to download all photos.');
      } finally {
        this.downloading.set(false);
      }
    })();
  }

  protected displayName(match: SelfieSearchMatch): string {
    return this.filenameFromUrl(match.url);
  }

  private filenameFromUrl(url: string): string {
    if (!url) return 'photo.jpg';
    try {
      const parsed = new URL(url);
      const pathParts = parsed.pathname.split('/').filter(Boolean);
      const last = pathParts[pathParts.length - 1];
      return last ? decodeURIComponent(last) : 'photo.jpg';
    } catch {
      const parts = url.split('/').filter(Boolean);
      const last = parts[parts.length - 1];
      return last ? decodeURIComponent(last) : 'photo.jpg';
    }
  }
}
