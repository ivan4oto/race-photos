import { InjectionToken } from '@angular/core';
import { environment } from '../../environments/environment';

export interface ApiConfig {
  protocol: string;
  host: string;
  port: number;
  basePath: string; // e.g. '/api'
}

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL');

export function buildApiBaseUrl(cfg: ApiConfig): string {
  const portPart = cfg.port ? `:${cfg.port}` : '';
  return `${cfg.protocol}://${cfg.host}${portPart}${cfg.basePath}`;
}

export const provideApiBaseUrl = () => ({ provide: API_BASE_URL, useValue: buildApiBaseUrl(environment.api) });
