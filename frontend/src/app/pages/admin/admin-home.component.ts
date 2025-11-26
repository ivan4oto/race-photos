import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface AdminLink {
  title: string;
  description: string;
  href: string;
  cta: string;
}

@Component({
  selector: 'app-admin-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-home.component.html',
  styleUrls: ['./admin-home.component.css'],
})
export class AdminHomeComponent {
  links: AdminLink[] = [
    {
      title: 'Events',
      description: 'Browse, edit, and assign photographers. Create new events.',
      href: '/admin/events',
      cta: 'Go to events',
    },
    {
      title: 'Create event',
      description: 'Launch a new event with pricing, access control, and metadata.',
      href: '/admin/events/new',
      cta: 'Create event',
    },
    {
      title: 'Photographers',
      description: 'Manage photographer profiles and payouts.',
      href: '/admin/photographers',
      cta: 'Go to photographers',
    },
    {
      title: 'Create photographer',
      description: 'Onboard a new photographer with rate card and payout prefs.',
      href: '/admin/photographers/new',
      cta: 'Create photographer',
    },
    {
      title: 'Participant providers',
      description: 'Maintain registration providers for participant imports.',
      href: '/admin/providers',
      cta: 'Go to providers',
    },
    {
      title: 'Create provider',
      description: 'Add a new provider for participant registration imports.',
      href: '/admin/providers/new',
      cta: 'Create provider',
    },
  ];
}
