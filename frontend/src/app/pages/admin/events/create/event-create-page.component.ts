import { CommonModule } from '@angular/common';
import {Component, OnInit, signal} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  CreateEventRequest,
  EligibilityMode,
  EventAdminService,
  EventStatus,
} from '../../../../shared/event-admin.service';
import { EventOrganizerAdminService, EventOrganizerResponse } from '../../../../shared/event-organizer-admin.service';

interface EventFormValue extends CreateEventRequest {}

@Component({
  selector: 'app-event-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styleUrls: ['./event-create-page.component.css'],
  templateUrl: './event-create-page.component.html',
})
export class EventCreatePageComponent implements OnInit{
  readonly statuses: EventStatus[] = [
    'DRAFT',
    'UPCOMING',
    'ACTIVE',
    'COMPLETED',
    'ARCHIVED',
  ];

  readonly eligibilityModes: EligibilityMode[] = [
    'NONE',
    'ROSTER_UPLOAD',
    'REGISTRATION_PROVIDER',
    'MANUAL_APPROVAL',
  ];

  organizers = signal<EventOrganizerResponse[]>([]);
  organizerError = signal<string | null>(null);
  loadingOrganizers = signal(true);
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
    name: this.fb.control('', {
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    description: this.fb.control('', { validators: [Validators.maxLength(4000)] }),
    status: this.fb.control<EventStatus>('DRAFT', { validators: [Validators.required] }),
    organizerId: this.fb.control<string | null>(''),
    organizerName: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    registrationProvider: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    vectorCollectionId: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    uploadPrefix: this.fb.control('', { validators: [Validators.maxLength(255)] }),
    timezone: this.fb.control('Europe/Berlin', { validators: [Validators.maxLength(60)] }),
    startTime: this.fb.control<string | null>(null),
    endTime: this.fb.control<string | null>(null),
    locationName: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    locationCity: this.fb.control('', { validators: [Validators.maxLength(120)] }),
    locationState: this.fb.control('', { validators: [Validators.maxLength(120)] }),
    locationCountry: this.fb.control('', { validators: [Validators.maxLength(120)] }),
    coverImageKey: this.fb.control('', { validators: [Validators.maxLength(255)] }),
    platformCommissionRate: this.fb.control<number | null>(null, {
      validators: [Validators.min(0), Validators.max(1)],
    }),
    watermarkingEnabled: this.fb.control(true),
    autoPublishMatches: this.fb.control(false),
    defaultPricing: this.fb.group({
      pricePerPhoto: this.fb.control<number | null>(null, [Validators.min(0)]),
      bundlePrice: this.fb.control<number | null>(null, [Validators.min(0)]),
      bundleSize: this.fb.control<number | null>(null, [Validators.min(1)]),
      currencyCode: this.fb.control('EUR', [Validators.maxLength(3)]),
    }),
    accessPolicy: this.fb.group({
      mode: this.fb.control<EligibilityMode>('NONE', { validators: [Validators.required] }),
      provider: this.fb.control('', { validators: [Validators.maxLength(120)] }),
      configuration: this.fb.control('', { validators: [Validators.maxLength(4000)] }),
    }),
    participantMessage: this.fb.control('', { validators: [Validators.maxLength(4000)] }),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly eventAdminService: EventAdminService,
    private readonly organizerAdminService: EventOrganizerAdminService,
  ) {}

  ngOnInit(): void {
    this.loadOrganizers();
  }

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
      await this.eventAdminService.createEvent(payload);
      this.submissionSuccess.set('Event created successfully.');
      this.form.reset({
        status: 'DRAFT',
        timezone: 'Europe/Berlin',
        defaultPricing: { currencyCode: 'EUR' },
        accessPolicy: { mode: 'NONE' },
        watermarkingEnabled: true,
        autoPublishMatches: false,
      });
    } catch (err) {
      console.error(err);
      this.submissionError.set('Unable to create event right now. Please retry.');
    } finally {
      this.submitting.set(false);
    }
  }

  private async loadOrganizers(): Promise<void> {
    this.loadingOrganizers.set(true);
    this.organizerError.set(null);
    try {
      const options = await this.organizerAdminService.listOrganizers();
      this.organizers.set(options);
    } catch (err) {
      console.error(err);
      this.organizerError.set('Unable to load organizers.');
    } finally {
      this.loadingOrganizers.set(false);
    }
  }

  private buildPayload(): EventFormValue {
    const raw = this.form.getRawValue();
    const organizerId = this.cleanString(raw.organizerId);
    return {
      slug: raw.slug!.trim(),
      name: raw.name!.trim(),
      description: this.cleanString(raw.description),
      status: raw.status!,
      organizerId: organizerId,
      organizerName: organizerId ? this.lookupOrganizerSlug(organizerId) : this.cleanString(raw.organizerName),
      registrationProvider: this.cleanString(raw.registrationProvider),
      vectorCollectionId: this.cleanString(raw.vectorCollectionId),
      uploadPrefix: this.cleanString(raw.uploadPrefix),
      timezone: this.cleanString(raw.timezone),
      startTime: this.cleanDateTime(raw.startTime),
      endTime: this.cleanDateTime(raw.endTime),
      locationName: this.cleanString(raw.locationName),
      locationCity: this.cleanString(raw.locationCity),
      locationState: this.cleanString(raw.locationState),
      locationCountry: this.cleanString(raw.locationCountry),
      coverImageKey: this.cleanString(raw.coverImageKey),
      platformCommissionRate: this.cleanNumber(raw.platformCommissionRate),
      watermarkingEnabled: !!raw.watermarkingEnabled,
      autoPublishMatches: !!raw.autoPublishMatches,
      defaultPricing: {
        pricePerPhoto: this.cleanNumber(raw.defaultPricing?.pricePerPhoto),
        bundlePrice: this.cleanNumber(raw.defaultPricing?.bundlePrice),
        bundleSize: this.cleanNumber(raw.defaultPricing?.bundleSize),
        currencyCode: this.cleanString(raw.defaultPricing?.currencyCode?.toUpperCase()),
      },
      accessPolicy: {
        mode: raw.accessPolicy?.mode ?? 'NONE',
        provider: this.cleanString(raw.accessPolicy?.provider),
        configuration: this.cleanString(raw.accessPolicy?.configuration),
      },
      participantMessage: this.cleanString(raw.participantMessage),
    };
  }

  private cleanString(value: unknown): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    const str = String(value).trim();
    return str.length ? str : null;
  }

  private lookupOrganizerSlug(id: string | null): string | null {
    if (!id) {
      return null;
    }
    const match = this.organizers().find(o => o.id === id);
    return match ? match.slug : null;
  }

  private cleanNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const num = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(num) ? num : null;
  }

  private cleanDateTime(value: string | null | undefined): string | null {
    if (!value) {
      return null;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date.toISOString();
  }
}
