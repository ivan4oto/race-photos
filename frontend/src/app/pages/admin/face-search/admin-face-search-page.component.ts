import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminFaceSearchMatch, AdminFaceSearchService } from '../../../shared/admin-face-search.service';

@Component({
  selector: 'app-admin-face-search-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-face-search-page.component.html',
  styleUrls: ['./admin-face-search-page.component.css']
})
export class AdminFaceSearchPageComponent implements OnInit {
  protected readonly eventId = signal<string | null>(null);
  protected readonly matches = signal<AdminFaceSearchMatch[]>([]);
  protected readonly probeKey = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly uploadError = signal<string | null>(null);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly selectedImage = signal<string | null>(null);

  protected readonly hasResults = computed(() => this.matches().length > 0);
  protected readonly canSubmit = computed(() => !!this.selectedFile() && !this.loading());

  constructor(
    private readonly route: ActivatedRoute,
    private readonly faceSearchService: AdminFaceSearchService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('eventId');
    this.eventId.set(id);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      this.selectedFile.set(null);
      return;
    }
    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.uploadError.set('Only image files are allowed.');
      this.selectedFile.set(null);
      return;
    }
    if (file.size > 6 * 1024 * 1024) {
      this.uploadError.set('Max file size is 6MB.');
      this.selectedFile.set(null);
      return;
    }
    this.uploadError.set(null);
    this.selectedFile.set(file);
  }

  async runSearch(): Promise<void> {
    const eventId = this.eventId();
    const file = this.selectedFile();
    if (!eventId || !file) {
      this.error.set('Choose a file before running the search.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.uploadError.set(null);
    try {
      const response = await this.faceSearchService.uploadProbe(eventId, file);
      this.matches.set(response.matches || []);
      this.probeKey.set(response.probePhotoKey || null);
      this.clearSelectedFile();
    } catch (err: any) {
      const apiMessage = err?.error?.message || err?.message;
      this.error.set(apiMessage || 'Face search failed. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }

  openImage(url: string): void {
    this.selectedImage.set(url);
  }

  closeModal(): void {
    this.selectedImage.set(null);
  }

  clearSelectedFile(): void {
    this.selectedFile.set(null);
    this.uploadError.set(null);
  }

  protected displayName(match: AdminFaceSearchMatch): string {
    return this.filenameFromUrl(match.url);
  }

  protected similarityLabel(match: AdminFaceSearchMatch): string {
    return `${match.similarity.toFixed(0)}%`;
  }

  private filenameFromUrl(url: string): string {
    if (!url) return 'photo.jpg';
    try {
      const parsed = new URL(url);
      const parts = parsed.pathname.split('/').filter(Boolean);
      const last = parts[parts.length - 1];
      return last ? decodeURIComponent(last) : 'photo.jpg';
    } catch {
      const pieces = url.split('/').filter(Boolean);
      const last = pieces[pieces.length - 1];
      return last ? decodeURIComponent(last) : 'photo.jpg';
    }
  }
}
