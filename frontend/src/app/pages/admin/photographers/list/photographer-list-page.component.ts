import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  PhotographerAdminService,
  PhotographerSummary,
} from '../../../../shared/photographer-admin.service';

@Component({
  selector: 'app-photographer-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  styleUrls: ['./photographer-list-page.component.css'],
  templateUrl: './photographer-list-page.component.html',
})
export class PhotographerListPageComponent implements OnInit {
  photographers = signal<PhotographerSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(private readonly photographerAdminService: PhotographerAdminService) {}

  ngOnInit(): void {
    this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const response = await this.photographerAdminService.listPhotographers();
      this.photographers.set(response);
    } catch (err) {
      console.error(err);
      this.error.set('Unable to load photographers right now.');
    } finally {
      this.loading.set(false);
    }
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
  }
}
