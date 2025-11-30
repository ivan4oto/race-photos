import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { PhotographerSummary } from './photographer-admin.service';

export type EventStatus =
  | 'DRAFT'
  | 'UPCOMING'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'ARCHIVED';

export type EligibilityMode =
  | 'NONE'
  | 'ROSTER_UPLOAD'
  | 'REGISTRATION_PROVIDER'
  | 'MANUAL_APPROVAL';

export interface PricingProfilePayload {
  pricePerPhoto: number | null;
  bundlePrice: number | null;
  bundleSize: number | null;
  currencyCode: string | null;
}

export interface EventAccessPolicyPayload {
  mode: EligibilityMode;
  provider: string | null;
  configuration: string | null;
}

export interface CreateEventRequest {
  slug: string;
  name: string;
  description: string | null;
  status: EventStatus;
  organizerName: string | null;
  registrationProvider: string | null;
  vectorCollectionId: string | null;
  uploadPrefix: string | null;
  timezone: string | null;
  startTime: string | null;
  endTime: string | null;
  locationName: string | null;
  locationCity: string | null;
  locationState: string | null;
  locationCountry: string | null;
  coverImageKey: string | null;
  platformCommissionRate: number | null;
  watermarkingEnabled: boolean;
  autoPublishMatches: boolean;
  defaultPricing: PricingProfilePayload;
  accessPolicy: EventAccessPolicyPayload;
  participantMessage: string | null;
}

export interface EventSummary {
  id: string;
  slug: string;
  name: string;
  status: EventStatus;
  startTime: string | null;
  endTime: string | null;
  locationCity: string | null;
  locationCountry: string | null;
  updatedAt: string;
  photographerCount: number;
}

export interface EventDetail extends CreateEventRequest {
  id: string;
  createdAt: string;
  updatedAt: string;
  indexedPhotoCount: number;
  unindexedPhotoCount: number;
  photographers: PhotographerSummary[];
}

export type UpdateEventRequest = CreateEventRequest;

export interface AddPhotographerToEventRequest {
  slug?: string | null;
  email?: string | null;
  firstName?: string | null;
  lastName?: string | null;
}

@Injectable({ providedIn: 'root' })
export class EventAdminService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  createEvent(payload: CreateEventRequest): Promise<void> {
    const url = `${this.apiBaseUrl}/admin/events`;
    return firstValueFrom(this.http.post<void>(url, payload));
  }

  listEvents(): Promise<EventSummary[]> {
    const url = `${this.apiBaseUrl}/admin/events`;
    return firstValueFrom(this.http.get<EventSummary[]>(url));
  }

  getEvent(id: string): Promise<EventDetail> {
    const url = `${this.apiBaseUrl}/admin/events/${id}`;
    return firstValueFrom(this.http.get<EventDetail>(url));
  }

  updateEvent(id: string, payload: UpdateEventRequest): Promise<EventDetail> {
    const url = `${this.apiBaseUrl}/admin/events/${id}`;
    return firstValueFrom(this.http.put<EventDetail>(url, payload));
  }

  addPhotographerToEvent(eventId: string, payload: AddPhotographerToEventRequest): Promise<EventDetail> {
    const url = `${this.apiBaseUrl}/admin/events/${eventId}/photographers`;
    return firstValueFrom(this.http.post<EventDetail>(url, payload));
  }

  removePhotographerFromEvent(eventId: string, photographerId: string): Promise<void> {
    const url = `${this.apiBaseUrl}/admin/events/${eventId}/photographers/${photographerId}`;
    return firstValueFrom(this.http.delete<void>(url));
  }

  indexFacesForEvent(eventId: string): Promise<void> {
    const url = `${this.apiBaseUrl}/admin/events/${eventId}/index-faces`;
    return firstValueFrom(this.http.post<void>(url, {}));
  }
}
