import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, Module, themeQuartz } from 'ag-grid-community';
import { EventAdminService } from '../../../../shared/event-admin.service';
import {DeleteButtonComponent} from "./delete-button.component";

interface PhotoPrefixRow {
  prefix: string;
  count: number;
}

@Component({
  selector: 'app-event-photos-page',
  standalone: true,
  imports: [CommonModule, RouterLink, AgGridAngular],
  templateUrl: './event-photos-page.component.html',
  styleUrls: ['./event-photos-page.component.css']
})
export class EventPhotosPageComponent implements OnInit {
  readonly modules: Module[] = [AllCommunityModule];
  readonly theme = themeQuartz;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<PhotoPrefixRow[]>([]);
  readonly eventId = signal<string>('');

  readonly columnDefs: ColDef[] = [
    { field: 'prefix', headerName: 'Prefix', flex: 2, minWidth: 220, cellClass: 'cell-strong' },
    { field: 'count', headerName: 'Items', maxWidth: 140, valueFormatter: params => this.formatCount(params.value) },
    {
      colId: 'actions',
      headerName: 'Delete',
      maxWidth: 140,
      sortable: false,
      filter: false,
      cellRenderer: DeleteButtonComponent
    }
  ];

  readonly defaultColDef: ColDef = {
    resizable: true,
    sortable: true,
    filter: true,
    flex: 1
  };

  constructor(
    private readonly route: ActivatedRoute,
    private readonly eventAdminService: EventAdminService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('eventId');
    if (id) {
      this.eventId.set(id);
      this.load();
    } else {
      this.error.set('Missing event id.');
    }
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const counts = await this.eventAdminService.getPhotoPrefixCounts(this.eventId());
      const mapped = Object.entries(counts || {}).map(([prefix, count]) => ({
        prefix,
        count: Number(count) || 0
      }));
      this.rows.set(mapped);
    } catch (err: any) {
      console.error(err);
      this.error.set(err?.error?.message || 'Unable to load photo prefixes right now.');
    } finally {
      this.loading.set(false);
    }
  }

  deleteAssetsWithPrefix(prefix: string): void {
    console.log('deleteAssetsWithPrefix not implemented yet for prefix', prefix);
  }

  formatCount(value: any): string {
    const num = Number(value);
    return Number.isNaN(num) ? '0' : num.toLocaleString();
  }
}
