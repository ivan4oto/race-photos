import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthSessionService } from '../../shared/auth/auth-session.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgIf],
  styleUrls: ['./header.component.css'],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  protected readonly authSession = inject(AuthSessionService);

  async onSignOut(): Promise<void> {
    await this.authSession.signOut();
  }
}
