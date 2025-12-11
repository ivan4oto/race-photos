import { CommonModule } from '@angular/common';
import {Component, Inject, OnInit, signal} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import {AllCommunityModule, CellClickedEvent, ColDef, Module, themeQuartz} from 'ag-grid-community';
import { EventAdminService } from '../../../../shared/event-admin.service';
import {DeleteButtonComponent} from "./delete-button.component";
import {API_BASE_URL} from "../../../../shared/api.config";
import {firstValueFrom} from "rxjs";
import {HttpClient} from "@angular/common/http";

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
      cellRenderer: DeleteButtonComponent,
      onCellClicked: (params) => this.deleteAssetsWithPrefix(params)
    }
  ];

  readonly defaultColDef: ColDef = {
    resizable: true,
    sortable: true,
    filter: true,
    flex: 1
  };

  constructor(
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string,
    private readonly route: ActivatedRoute,
    private readonly eventAdminService: EventAdminService,
    private readonly http: HttpClient
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

  async deleteAssetsWithPrefix(params: CellClickedEvent) {
    console.log('deleteAssetsWithPrefix not implemented yet for prefix');
    const prefix = params.data.prefix;
    this.loading.set(true);
    const url = `${this.apiBaseUrl}/admin/storage/delete-by-prefix`;
    try {
        const result = await firstValueFrom(
            this.http.post<DeleteByPrefixResponse>(url, { prefix })
        );
        const deletedS3 = result?.deletedS3Objects ?? 0;
        const deletedAssets = result?.deletedPhotoAssets ?? 0;
        console.log('deletedAssets', deletedAssets);
        console.log('deletedS3Objects', deletedS3);
    } catch (err: any) {
        const message = err?.error?.message || 'Failed to delete assets for this prefix.';
        console.error('Delete by prefix failed', err);
        this.error.set(message);
    } finally {
        this.loading.set(false);
    }
  }

  formatCount(value: any): string {
    const num = Number(value);
    return Number.isNaN(num) ? '0' : num.toLocaleString();
  }
}

interface DeleteByPrefixResponse {
  deletedS3Objects: number;
  deletedPhotoAssets: number;
}
