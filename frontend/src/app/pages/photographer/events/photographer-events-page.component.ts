import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AgGridAngular } from 'ag-grid-angular';
import {ColDef, Module, themeBalham} from 'ag-grid-community';
import { finalize } from 'rxjs/operators';
import { API_BASE_URL } from '../../../shared/api.config';
import { AllCommunityModule } from 'ag-grid-community';

interface PhotographerEvent {
  id: string;
  name: string;
  slug: string;
  startDate: string | null;
}

@Component({
  selector: 'app-photographer-events-page',
  standalone: true,
  imports: [CommonModule, AgGridAngular],
  templateUrl: './photographer-events-page.component.html',
  styleUrls: ['./photographer-events-page.component.css']
})
export class PhotographerEventsPageComponent implements OnInit {
  public modules: Module[] = [
      AllCommunityModule
  ]
  public theme = themeBalham;
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<PhotographerEvent[]>([]);

  readonly columnDefs: ColDef[] = [
    {
      field: 'name',
      headerName: 'Event',
      flex: 1.4,
      minWidth: 180,
      cellClass: 'cell-strong'
    },
    { field: 'slug', headerName: 'Slug', flex: 1, minWidth: 160 },
    {
      field: 'startDate',
      headerName: 'Start',
      flex: 1,
      minWidth: 170,
      valueFormatter: params => this.formatDate(params.value)
    },
    {
      colId: 'actions',
      headerName: 'Upload',
      width: 140,
      minWidth: 120,
      suppressMovable: true,
      sortable: false,
      filter: false,
      cellRenderer: () => `<button class="btn primary">Upload</button>`,
      cellClass: 'action-cell'
    }
  ];

  readonly defaultColDef: ColDef = {
    resizable: true,
    sortable: true,
    filter: true,
    flex: 1
  };

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {
  }

  ngOnInit(): void {
    this.load();
  }

  onCellClicked(event: any): void {
    if (event?.colDef?.colId === 'actions' && event?.data?.slug) {
      this.router.navigate(['/photographer/upload', event.data.slug]);
    }
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<PhotographerEvent[]>(`${this.apiBaseUrl}/photographer/events`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: res => this.events.set(res || []),
        error: (err: HttpErrorResponse) => {
          if (err.status === 401) {
            this.error.set('Please sign in to view your events.');
          } else if (err.status === 403) {
            this.error.set('You do not have access to photographer events.');
          } else {
            this.error.set(err.error?.message || 'Could not load events right now.');
          }
        }
      });
  }

  formatDate(value: string | null | undefined): string {
    if (!value) return 'TBD';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'TBD';
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }
}
