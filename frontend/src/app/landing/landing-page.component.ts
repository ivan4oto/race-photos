import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  styleUrls: ['./landing-page.component.css'],
  templateUrl: './landing-page.component.html'
})
export class LandingPageComponent {
  constructor(private router: Router) {}

  onPhotographerStart() {
    this.router.navigate(['/photographer']);
  }

  onParticipantBrowse() {
    this.router.navigate(['/participants']);
  }
}
