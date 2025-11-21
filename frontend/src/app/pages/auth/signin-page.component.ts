import { Component, inject } from '@angular/core';
import { AmplifyAuthenticatorModule } from '@aws-amplify/ui-angular';
import { NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { AuthSessionService } from '../../shared/auth/auth-session.service';

@Component({
  selector: 'app-signin-page',
  standalone: true,
  imports: [AmplifyAuthenticatorModule, NgIf],
  templateUrl: './signin-page.component.html',
  styleUrls: ['./signin-page.component.css']
})
export class SignInPageComponent {
  protected readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);
  protected readonly formFields = {
    signUp: {
      email: { order: 1 },
      given_name: { order: 2, label: 'First name', placeholder: 'First name', isRequired: true },
      family_name: { order: 3, label: 'Last name', placeholder: 'Last name', isRequired: true }
    }
  };

  protected continueToApp(): void {
    void this.router.navigateByUrl('/');
  }
}
