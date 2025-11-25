import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import {
  PhotographerAdminService,
  PhotographerStatus,
  PayoutMethod,
  CreatePhotographerRequest,
} from '../../../../shared/photographer-admin.service';

interface PhotographerFormValue extends CreatePhotographerRequest {}

@Component({
  selector: 'app-photographer-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styleUrls: ['./photographer-create-page.component.css'],
  templateUrl: './photographer-create-page.component.html',
})
export class PhotographerCreatePageComponent {
  readonly statuses: PhotographerStatus[] = [
    'ONBOARDING',
    'ACTIVE',
    'PAUSED',
    'SUSPENDED',
  ];

  readonly payoutMethods: PayoutMethod[] = [
    'UNSPECIFIED',
    'ACH',
    'PAYPAL',
    'WISE',
    'STRIPE_CONNECT',
    'MANUAL',
  ];

  submitting = signal(false);
  submissionError = signal<string | null>(null);
  submissionSuccess = signal<string | null>(null);

  readonly form = this.fb.group({
    slug: this.fb.control('', {
      validators: [
        Validators.required,
        Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
        Validators.maxLength(80),
      ],
    }),
    firstName: this.fb.control('', {
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    lastName: this.fb.control('', {
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    displayName: this.fb.control('', {
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    email: this.fb.control('', {
      validators: [Validators.required, Validators.email, Validators.maxLength(160)],
    }),
    phoneNumber: this.fb.control('', { validators: [Validators.maxLength(40)] }),
    studioName: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    website: this.fb.control('', { validators: [Validators.maxLength(255)] }),
    defaultCurrency: this.fb.control('EUR', {
      validators: [Validators.maxLength(3)],
    }),
    status: this.fb.control<PhotographerStatus>('ONBOARDING', {
      validators: [Validators.required],
    }),
    biography: this.fb.control(''),
    commissionOverride: this.fb.control<number | null>(null, {
      validators: [Validators.min(0), Validators.max(1)],
    }),
    rateCard: this.fb.group({
      pricePerPhoto: this.fb.control<number | null>(null, [Validators.min(0)]),
      bundlePrice: this.fb.control<number | null>(null, [Validators.min(0)]),
      bundleSize: this.fb.control<number | null>(null, [Validators.min(1)]),
      currencyCode: this.fb.control('EUR', [Validators.maxLength(3)]),
    }),
    payoutPreferences: this.fb.group({
      method: this.fb.control<PayoutMethod>('UNSPECIFIED', {
        validators: [Validators.required],
      }),
      accountReference: this.fb.control('', { validators: [Validators.maxLength(255)] }),
      payoutEmail: this.fb.control('', { validators: [Validators.email, Validators.maxLength(160)] }),
      bankAccountLast4: this.fb.control('', { validators: [Validators.maxLength(4)] }),
      bankRoutingNumber: this.fb.control('', { validators: [Validators.maxLength(9)] }),
      taxId: this.fb.control('', { validators: [Validators.maxLength(64)] }),
      metadata: this.fb.control('', { validators: [Validators.maxLength(2000)] }),
    }),
    payoutThreshold: this.fb.control<number | null>(null, [Validators.min(0)]),
    internalNotes: this.fb.control('', { validators: [Validators.maxLength(4000)] }),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly adminService: PhotographerAdminService,
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

    this.submissionError.set(null);
    this.submissionSuccess.set(null);
    this.submitting.set(true);
    const payload = this.buildPayload();

    try {
      await this.adminService.createPhotographer(payload);
      this.submissionSuccess.set('Photographer profile created successfully.');
      this.form.reset({
        status: 'ONBOARDING',
        defaultCurrency: 'EUR',
        rateCard: {
          currencyCode: 'EUR',
        },
        payoutPreferences: {
          method: 'UNSPECIFIED',
        },
      });
    } catch (err) {
      console.error(err);
      this.submissionError.set('Failed to create photographer. Please try again shortly.');
    } finally {
      this.submitting.set(false);
    }
  }

  private buildPayload(): PhotographerFormValue {
    const raw = this.form.getRawValue();
    return {
      slug: raw.slug!.trim(),
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      displayName: raw.displayName!.trim(),
      email: raw.email!.trim(),
      phoneNumber: this.cleanString(raw.phoneNumber),
      studioName: this.cleanString(raw.studioName),
      website: this.cleanString(raw.website),
      defaultCurrency: this.cleanString(raw.defaultCurrency?.toUpperCase()),
      status: raw.status!,
      biography: this.cleanString(raw.biography),
      commissionOverride: this.cleanNumber(raw.commissionOverride),
      rateCard: {
        pricePerPhoto: this.cleanNumber(raw.rateCard?.pricePerPhoto),
        bundlePrice: this.cleanNumber(raw.rateCard?.bundlePrice),
        bundleSize: this.cleanNumber(raw.rateCard?.bundleSize),
        currencyCode: this.cleanString(raw.rateCard?.currencyCode?.toUpperCase()),
      },
      payoutPreferences: {
        method: raw.payoutPreferences?.method ?? 'UNSPECIFIED',
        accountReference: this.cleanString(raw.payoutPreferences?.accountReference),
        payoutEmail: this.cleanString(raw.payoutPreferences?.payoutEmail),
        bankAccountLast4: this.cleanString(raw.payoutPreferences?.bankAccountLast4),
        bankRoutingNumber: this.cleanString(raw.payoutPreferences?.bankRoutingNumber),
        taxId: this.cleanString(raw.payoutPreferences?.taxId),
        metadata: this.cleanString(raw.payoutPreferences?.metadata),
      },
      payoutThreshold: this.cleanNumber(raw.payoutThreshold),
      internalNotes: this.cleanString(raw.internalNotes),
    };
  }

  private cleanString(value: unknown): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    const str = String(value).trim();
    return str.length ? str : null;
  }

  private cleanNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const num = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(num) ? num : null;
  }
}
