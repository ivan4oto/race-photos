import { Component } from '@angular/core';

@Component({
  selector: 'app-signin-page',
  standalone: true,
  template: `
    <section class="page">
      <div class="container">
        <h1>Sign in</h1>
        <p>Authentication coming soon. This link is a placeholder.</p>
      </div>
    </section>
  `,
  styles: [`
    .page { background: #ffffff; padding: 32px 16px; }
    .container { max-width: 720px; margin: 0 auto; }
    h1 { margin: 0 0 8px; font-size: clamp(26px, 4vw, 36px); }
  `]
})
export class SignInPageComponent {}

