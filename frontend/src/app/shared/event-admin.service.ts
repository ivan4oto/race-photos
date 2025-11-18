import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';

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
}
