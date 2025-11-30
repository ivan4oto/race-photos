import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy, WritableSignal, effect, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { fetchAuthSession, signOut as amplifySignOut } from 'aws-amplify/auth';
import { Hub } from 'aws-amplify/utils';
import { API_BASE_URL } from '../api.config';

export interface AuthenticatedUser {
  email?: string;
  picture?: string;
  givenName?: string;
  familyName?: string;
  roles?: string[];
}

type SessionStatus = 'idle' | 'linking' | 'error';

interface CreateSessionPayload {
  idToken: string;
  accessToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  private readonly userSignal: WritableSignal<AuthenticatedUser | null> = signal<AuthenticatedUser | null>(null);
  private readonly statusSignal: WritableSignal<SessionStatus> = signal<SessionStatus>('idle');
  private hubUnsubscribe = Hub.listen('auth', ({ payload }) => {
    if (payload.event === 'signedIn') {
      void this.linkBackendSession();
    }
    if (payload.event === 'signedOut') {
      void this.clearBackendSession();
    }
  });

  readonly user = this.userSignal.asReadonly();
  readonly status = this.statusSignal.asReadonly();

  constructor() {
    // Try to load an existing Spring session (cookie) first.
    void this.restoreBackendSession();
    // If Cognito already has tokens (e.g. returning from redirect) ensure backend is synced.
    void this.tryLinkFromExistingAmplifySession();

    // Keep the app aware if a session is lost.
    effect(() => {
      if (this.statusSignal() === 'error') {
        console.warn('Cognito session could not be linked to backend.');
      }
    });
  }

  ngOnDestroy(): void {
    this.hubUnsubscribe?.();
  }

  private async restoreBackendSession(): Promise<void> {
    try {
      const user = await firstValueFrom(this.http.get<AuthenticatedUser>(`${this.apiBaseUrl}/auth/session`));
      this.userSignal.set(user);
    } catch {
      this.userSignal.set(null);
    }
  }

  private async tryLinkFromExistingAmplifySession(): Promise<void> {
    try {
      const session = await fetchAuthSession();
      if (session.tokens && !this.userSignal()) {
        await this.linkBackendSession(session.tokens);
      }
    } catch {
      // Not signed in yet — ignore.
    }
  }

  private async linkBackendSession(tokens?: NonNullable<Awaited<ReturnType<typeof fetchAuthSession>>['tokens']>): Promise<void> {
    this.statusSignal.set('linking');
    try {
      const resolvedTokens = tokens ?? (await fetchAuthSession({ forceRefresh: true })).tokens;
      if (!resolvedTokens?.idToken || !resolvedTokens?.accessToken) {
        throw new Error('Missing Cognito tokens');
      }

      const payload: CreateSessionPayload = {
        idToken: resolvedTokens.idToken.toString(),
        accessToken: resolvedTokens.accessToken.toString()
      };

      const user = await firstValueFrom(
        this.http.post<AuthenticatedUser>(`${this.apiBaseUrl}/auth/session`, payload)
      );
      this.userSignal.set(user);
      this.statusSignal.set('idle');
    } catch (error) {
      console.error('Failed to link Cognito session', error);
      this.userSignal.set(null);
      this.statusSignal.set('error');
    }
  }

  private async clearBackendSession(): Promise<void> {
    try {
      await firstValueFrom(this.http.delete<void>(`${this.apiBaseUrl}/auth/session`));
    } catch {
      // Intentionally ignore errors — cookie might already be gone.
    } finally {
      this.userSignal.set(null);
      this.statusSignal.set('idle');
    }
  }

  async signOut(): Promise<void> {
    await amplifySignOut();
    await this.clearBackendSession();
  }
}
