import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, Routes, withInMemoryScrolling } from '@angular/router';
import { Amplify } from 'aws-amplify';
import { AppComponent } from './app/app.component';
import { provideApiBaseUrl } from './app/shared/api.config';
import { environment } from './environments/environment';
import { apiCredentialsInterceptor } from './app/shared/api-credentials.interceptor';
import { adminGuard } from './app/shared/auth/admin.guard';
import { photographerGuard } from './app/shared/auth/photographer.guard';

Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: environment.auth.userPoolId,
      userPoolClientId: environment.auth.userPoolClientId,
      loginWith: {
        oauth: {
          domain: environment.auth.domain,
          scopes: environment.auth.scopes,
          redirectSignIn: [environment.auth.redirectSignIn],
          redirectSignOut: [environment.auth.redirectSignOut],
          responseType: 'code'
        }
      }
    }
  }
});

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./app/landing/landing-page.component').then(m => m.LandingPageComponent),
    title: 'Race Photos — Find and sell event photos'
  },
  {
    path: 'event/:eventId',
    loadComponent: () => import('./app/pages/event/event-gallery-page.component').then(m => m.EventGalleryPageComponent),
    title: 'My Event Photos — Race Photos'
  },
  {
    path: 'photographer',
    loadComponent: () => import('./app/pages/photographer/photographer-page.component').then(m => m.PhotographerPageComponent),
    title: 'For Photographers — Race Photos'
  },
  {
    path: 'photographer/events',
    loadComponent: () => import('./app/pages/photographer/events/photographer-events-page.component').then(m => m.PhotographerEventsPageComponent),
    title: 'My Events — Race Photos',
    // canMatch: [photographerGuard]
  },
  {
    path: 'photographer/upload/:eventSlug',
    loadComponent: () => import('./app/pages/photographer-upload/upload-page.component').then(m => m.UploadPageComponent),
    title: 'Upload — Race Photos'
  },
  {
    path: 'participants',
    loadComponent: () => import('./app/pages/participants/participants-page.component').then(m => m.ParticipantsPageComponent),
    title: 'For Participants — Race Photos'
  },
  {
    path: 'participants/face-search-test',
    loadComponent: () => import('./app/pages/participants/face-search-test.component').then(m => m.FaceSearchTestComponent),
    title: 'Face Search Test — Race Photos'
  },
  {
    path: 'admin/photographers/new',
    loadComponent: () => import('./app/pages/admin/photographers/create/photographer-create-page.component').then(m => m.PhotographerCreatePageComponent),
    title: 'Create Photographer — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/providers/new',
    loadComponent: () => import('./app/pages/admin/providers/create/provider-create-page.component').then(m => m.ProviderCreatePageComponent),
    title: 'Create Provider — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/providers',
    loadComponent: () => import('./app/pages/admin/providers/list/provider-list-page.component').then(m => m.ProviderListPageComponent),
    title: 'Providers — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/organizers/new',
    loadComponent: () => import('./app/pages/admin/organizers/organizer-create-page.component').then(m => m.OrganizerCreatePageComponent),
    title: 'Create Event Organizer — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/organizers',
    loadComponent: () => import('./app/pages/admin/organizers/organizer-list-page.component').then(m => m.OrganizerListPageComponent),
    title: 'Event Organizers — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/organizers/:organizerId',
    loadComponent: () => import('./app/pages/admin/organizers/organizer-edit-page.component').then(m => m.OrganizerEditPageComponent),
    title: 'Edit Event Organizer — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin',
    loadComponent: () => import('./app/pages/admin/admin-home.component').then(m => m.AdminHomeComponent),
    title: 'Admin — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/photographers',
    loadComponent: () => import('./app/pages/admin/photographers/list/photographer-list-page.component').then(m => m.PhotographerListPageComponent),
    title: 'Photographers — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/photographers/:photographerId',
    loadComponent: () => import('./app/pages/admin/photographers/edit/photographer-edit-page.component').then(m => m.PhotographerEditPageComponent),
    title: 'Edit Photographer — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/events/new',
    loadComponent: () => import('./app/pages/admin/events/create/event-create-page.component').then(m => m.EventCreatePageComponent),
    title: 'Create Event — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/events',
    loadComponent: () => import('./app/pages/admin/events/list/event-list-page.component').then(m => m.EventListPageComponent),
    title: 'Events — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/events/:eventId/photos',
    loadComponent: () => import('./app/pages/admin/events/photos/event-photos-page.component').then(m => m.EventPhotosPageComponent),
    title: 'Event Photos — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'admin/events/:eventId',
    loadComponent: () => import('./app/pages/admin/events/edit/event-edit-page.component').then(m => m.EventEditPageComponent),
    title: 'Edit Event — Race Photos',
    canMatch: [adminGuard]
  },
  {
    path: 'signin',
    loadComponent: () => import('./app/pages/auth/signin-page.component').then(m => m.SignInPageComponent),
    title: 'Sign in — Race Photos'
  },
  {
    path: 'signin/callback',
    loadComponent: () => import('./app/pages/auth/signin-page.component').then(m => m.SignInPageComponent),
    title: 'Sign in — Race Photos'
  },
  { path: '**', redirectTo: '' }
];

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptors([apiCredentialsInterceptor])),
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' })),
    provideApiBaseUrl(),
  ],
}).catch(err => console.error(err));
