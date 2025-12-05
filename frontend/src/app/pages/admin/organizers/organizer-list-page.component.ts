import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { EventOrganizerAdminService, EventOrganizerResponse } from '../../../shared/event-organizer-admin.service';

@Component({
  selector: 'app-organizer-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  styleUrls: ['./event-organizer-list-page.component.css'],
  templateUrl: './organizer-list-page.component.html',
})
export class OrganizerListPageComponent implements OnInit {
  organizers = signal<EventOrganizerResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(
    private readonly organizerAdminService: EventOrganizerAdminService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const response = await this.organizerAdminService.listOrganizers();
      this.organizers.set(response);
    } catch (err: any) {
      console.error(err);
      if (err?.status === 403) {
        this.error.set('You do not have permission to view organizers.');
      } else {
        this.error.set('Unable to load organizers right now.');
      }
    } finally {
      this.loading.set(false);
    }
  }

  goToOrganizer(id: string): void {
    this.router.navigate(['/admin/organizers', id]);
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
  }
}
