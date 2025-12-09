import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { AuthSessionService } from './auth-session.service';

const isPhotographerOrAdmin = (roles?: string[] | null): boolean => {
  if (!roles) return false;
  return roles.includes('PHOTOGRAPHER') || roles.includes('ADMIN');
};

export const photographerGuard: CanMatchFn = (): boolean | UrlTree => {
  const auth = inject(AuthSessionService);
  const router = inject(Router);

  const user = auth.user();
  if (user && isPhotographerOrAdmin(user.roles)) {
    return true;
  }

  return router.parseUrl('/');
};
