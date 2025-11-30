import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventAdminService, EventSummary } from '../../../shared/event-admin.service';

@Component({
  selector: 'app-event-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  styleUrls: ['./event-list-page.component.css'],
  templateUrl: './event-list-page.component.html',
})
export class EventListPageComponent implements OnInit {
  events = signal<EventSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(private readonly eventAdminService: EventAdminService) {}

  ngOnInit(): void {
    this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const response = await this.eventAdminService.listEvents();
      this.events.set(response);
    } catch (err: any) {
      console.error(err);
      if (err.status === 403) {
        this.error.set('You do not have permission to view events.');
      } else {
        this.error.set('Unable to load events right now.');
      }
    } finally {
      this.loading.set(false);
    }
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'TBD';
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'TBD' : date.toLocaleString();
  }
}
