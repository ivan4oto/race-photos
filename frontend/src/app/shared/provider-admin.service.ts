import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';

export interface CreateProviderRequest {
  displayName: string;
  email: string | null;
  website: string | null;
}

export interface ProviderSummary {
  id: string;
  displayName: string;
  email: string | null;
  website: string | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ProviderAdminService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private readonly apiBaseUrl: string
  ) {}

  createProvider(payload: CreateProviderRequest): Promise<void> {
    const url = `${this.apiBaseUrl}/admin/providers`;
    return firstValueFrom(this.http.post<void>(url, payload));
  }

  listProviders(): Promise<ProviderSummary[]> {
    const url = `${this.apiBaseUrl}/admin/providers`;
    return firstValueFrom(this.http.get<ProviderSummary[]>(url));
  }
}
