import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Routes, withInMemoryScrolling } from '@angular/router';
import { AppComponent } from './app/app.component';
import { provideApiBaseUrl } from './app/shared/api.config';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./app/landing/landing-page.component').then(m => m.LandingPageComponent),
    title: 'Race Photos — Find and sell event photos'
  },
  {
    path: 'photographer',
    loadComponent: () => import('./app/pages/photographer/photographer-page.component').then(m => m.PhotographerPageComponent),
    title: 'For Photographers — Race Photos'
  },
  {
    path: 'photographer/upload',
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
    loadComponent: () => import('./app/pages/admin/photographers/photographer-create-page.component').then(m => m.PhotographerCreatePageComponent),
    title: 'Create Photographer — Race Photos'
  },
  {
    path: 'signin',
    loadComponent: () => import('./app/pages/auth/signin-page.component').then(m => m.SignInPageComponent),
    title: 'Sign in — Race Photos'
  },
  { path: '**', redirectTo: '' }
];

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' })),
    provideApiBaseUrl(),
  ],
}).catch(err => console.error(err));
