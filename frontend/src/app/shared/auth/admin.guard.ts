import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { AuthSessionService } from './auth-session.service';

const isAdmin = (roles?: string[] | null): boolean => {
  if (!roles) return false;
  return roles.includes('ADMIN');
};

export const adminGuard: CanMatchFn = (): boolean | UrlTree => {
  const auth = inject(AuthSessionService);
  const router = inject(Router);

  const user = auth.user();
  if (user && isAdmin(user.roles)) {
    return true;
  }

  // Redirect non-admins to home; could also send to signin if anonymous.
  return router.parseUrl('/');
};
