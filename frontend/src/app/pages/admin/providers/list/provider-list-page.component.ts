import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProviderAdminService, ProviderSummary } from '../../../../shared/provider-admin.service';

@Component({
  selector: 'app-provider-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './provider-list-page.component.html',
  styleUrls: ['./provider-list-page.component.css'],
})
export class ProviderListPageComponent implements OnInit {
  providers = signal<ProviderSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(private readonly providerAdminService: ProviderAdminService) {}

  ngOnInit(): void {
    this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await this.providerAdminService.listProviders();
      this.providers.set(result);
    } catch (err) {
      console.error(err);
      this.error.set('Unable to load providers right now.');
    } finally {
      this.loading.set(false);
    }
  }

  formatDate(value: string | null | undefined): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
  }
}
