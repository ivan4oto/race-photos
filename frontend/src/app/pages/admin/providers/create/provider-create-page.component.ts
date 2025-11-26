import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ProviderAdminService } from '../../../../shared/provider-admin.service';

@Component({
  selector: 'app-provider-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './provider-create-page.component.html',
  styleUrls: ['./provider-create-page.component.css'],
})
export class ProviderCreatePageComponent {
  submitting = signal(false);
  submissionError = signal<string | null>(null);
  submissionSuccess = signal<string | null>(null);

  readonly form = this.fb.group({
    displayName: this.fb.control('', {
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    email: this.fb.control('', {
      validators: [Validators.email, Validators.maxLength(160)],
    }),
    website: this.fb.control('', {
      validators: [Validators.maxLength(255)],
    }),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly providerAdminService: ProviderAdminService,
    private readonly router: Router
  ) {}

  controlInvalid(path: string): boolean {
    const control = this.form.get(path);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.submissionError.set(null);
    this.submissionSuccess.set(null);

    const payload = {
      displayName: this.form.value.displayName!.trim(),
      email: this.cleanString(this.form.value.email),
      website: this.cleanString(this.form.value.website),
    };

    try {
      await this.providerAdminService.createProvider(payload);
      this.submissionSuccess.set('Provider created successfully.');
      await this.router.navigate(['/admin/providers']);
    } catch (err) {
      console.error(err);
      this.submissionError.set('Unable to create provider right now.');
    } finally {
      this.submitting.set(false);
    }
  }

  private cleanString(value: unknown): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    const str = String(value).trim();
    return str.length ? str : null;
  }
}
