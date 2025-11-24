import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface PricingProfilePayload {
  pricePerPhoto: number | null;
  bundlePrice: number | null;
  bundleSize: number | null;
  currencyCode: string | null;
}

export type PhotographerStatus =
  | 'ONBOARDING'
  | 'ACTIVE'
  | 'PAUSED'
  | 'SUSPENDED';

export type PayoutMethod =
  | 'UNSPECIFIED'
  | 'ACH'
  | 'PAYPAL'
  | 'WISE'
  | 'STRIPE_CONNECT'
  | 'MANUAL';

export interface PayoutPreferencesPayload {
  method: PayoutMethod;
  accountReference: string | null;
  payoutEmail: string | null;
  bankAccountLast4: string | null;
  bankRoutingNumber: string | null;
  taxId: string | null;
  metadata: string | null;
}

export interface CreatePhotographerRequest {
  slug: string;
  firstName: string;
  lastName: string;
  displayName: string;
  email: string;
  phoneNumber: string | null;
  studioName: string | null;
  website: string | null;
  defaultCurrency: string | null;
  status: PhotographerStatus;
  biography: string | null;
  commissionOverride: number | null;
  rateCard: PricingProfilePayload;
  payoutPreferences: PayoutPreferencesPayload;
  payoutThreshold: number | null;
  internalNotes: string | null;
}

export interface PhotographerSummary {
  id: string;
  slug: string;
  displayName: string;
  email: string;
  status: PhotographerStatus;
  defaultCurrency: string | null;
  updatedAt: string;
}

export interface PhotographerDetail extends CreatePhotographerRequest {
  id: string;
  createdAt: string;
  updatedAt: string;
}

export type UpdatePhotographerRequest = CreatePhotographerRequest;

@Injectable({ providedIn: 'root' })
export class PhotographerAdminService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  createPhotographer(payload: CreatePhotographerRequest): Promise<void> {
    const url = `${this.apiBaseUrl}/admin/photographers`;
    return firstValueFrom(this.http.post<void>(url, payload));
  }

  listPhotographers(): Promise<PhotographerSummary[]> {
    const url = `${this.apiBaseUrl}/admin/photographers`;
    return firstValueFrom(this.http.get<PhotographerSummary[]>(url));
  }

  getPhotographer(id: string): Promise<PhotographerDetail> {
    const url = `${this.apiBaseUrl}/admin/photographers/${id}`;
    return firstValueFrom(this.http.get<PhotographerDetail>(url));
  }

  updatePhotographer(id: string, payload: UpdatePhotographerRequest): Promise<PhotographerDetail> {
    const url = `${this.apiBaseUrl}/admin/photographers/${id}`;
    return firstValueFrom(this.http.put<PhotographerDetail>(url, payload));
  }
}
