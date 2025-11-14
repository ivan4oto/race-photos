import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { API_BASE_URL } from '../../shared/api.config';

interface FaceSearchMatch {
  photoKey: string;
  faceId: string;
  similarity: number;
  confidence?: number | null;
  boundingBox?: {
    width: number | null;
    height: number | null;
    left: number | null;
    top: number | null;
  } | null;
}

interface FaceSearchResponse {
  eventId: string;
  queryPhotoKey: string;
  matches: FaceSearchMatch[];
}

@Component({
  selector: 'app-face-search-test',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styleUrls: ['./face-search-test.component.css'],
  templateUrl: './face-search-test.component.html'
})
export class FaceSearchTestComponent {
  readonly form = this.fb.group({
    raceId: ['', Validators.required],
    photoKey: ['', Validators.required]
  });

  matches: FaceSearchMatch[] = [];
  loading = false;
  errorMessage = '';
  selectedMatch?: FaceSearchMatch;

  private readonly bucketUrl = 'https://race-photos-dev.s3.eu-central-1.amazonaws.com';

  constructor(
    private readonly fb: FormBuilder,
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  search(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = {
      eventId: this.form.value.raceId!.trim(),
      photoKey: this.form.value.photoKey!.trim()
    };

    this.loading = true;
    this.errorMessage = '';
    this.matches = [];

    this.http.post<FaceSearchResponse>(`${this.apiBaseUrl}/faces/search`, payload)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: response => {
          this.matches = response.matches ?? [];
          if (!this.matches.length) {
            this.errorMessage = 'No matches were returned.';
          }
        },
        error: err => {
          const apiMessage = err?.error?.message ?? err?.message;
          this.errorMessage = apiMessage || 'Search failed. Please try again.';
        }
      });
  }

  buildImageUrl(photoKey: string): string {
    return `${this.bucketUrl}/${encodeURI(photoKey)}`;
  }

  openPreview(match: FaceSearchMatch): void {
    this.selectedMatch = match;
  }

  closePreview(): void {
    this.selectedMatch = undefined;
  }
}
