import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';

export type EventOrganizerStatus = 'ACTIVE' | 'DISABLED';

export interface EventOrganizerRequest {
  slug: string;
  name: string;
  email: string;
  phoneNumber: string | null;
  status: EventOrganizerStatus;
}

export interface EventOrganizerResponse extends EventOrganizerRequest {
  id: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class EventOrganizerAdminService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  listOrganizers(): Promise<EventOrganizerResponse[]> {
    const url = `${this.apiBaseUrl}/admin/organizers`;
    return firstValueFrom(this.http.get<EventOrganizerResponse[]>(url));
  }

  getOrganizer(id: string): Promise<EventOrganizerResponse> {
    const url = `${this.apiBaseUrl}/admin/organizers/${id}`;
    return firstValueFrom(this.http.get<EventOrganizerResponse>(url));
  }

  createOrganizer(payload: EventOrganizerRequest): Promise<EventOrganizerResponse> {
    const url = `${this.apiBaseUrl}/admin/organizers`;
    return firstValueFrom(this.http.post<EventOrganizerResponse>(url, payload));
  }

  updateOrganizer(id: string, payload: EventOrganizerRequest): Promise<EventOrganizerResponse> {
    const url = `${this.apiBaseUrl}/admin/organizers/${id}`;
    return firstValueFrom(this.http.patch<EventOrganizerResponse>(url, payload));
  }

  disableOrganizer(id: string): Promise<void> {
    const url = `${this.apiBaseUrl}/admin/organizers/${id}`;
    return firstValueFrom(this.http.delete<void>(url));
  }
}
