import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_BASE_URL } from './api.config';

/**
 * Ensures cookies are sent when calling our backend API so Cognito-backed sessions work.
 */
export const apiCredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const apiBaseUrl = inject(API_BASE_URL);
  if (req.url.startsWith(apiBaseUrl)) {
    return next(req.clone({ withCredentials: true }));
  }
  return next(req);
};

