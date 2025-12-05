import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AddPhotographerToEventRequest,
  CreateEventRequest,
  EligibilityMode,
  EventAdminService,
  EventDetail,
  EventStatus,
} from '../../../shared/event-admin.service';
import { PhotographerSummary } from '../../../shared/photographer-admin.service';
import { EventOrganizerAdminService, EventOrganizerResponse } from '../../../shared/event-organizer-admin.service';

interface EventFormValue extends CreateEventRequest {}

@Component({
  selector: 'app-event-edit-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  styleUrls: ['./event-create-page.component.css', './event-edit-page.component.css'],
  templateUrl: './event-edit-page.component.html',
})
export class EventEditPageComponent implements OnInit {
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

  loadingEvent = signal(true);
  loadError = signal<string | null>(null);
  loadingOrganizers = signal(true);
  organizerError = signal<string | null>(null);
  saving = signal(false);
  submissionError = signal<string | null>(null);
  submissionSuccess = signal<string | null>(null);
  assigning = signal(false);
  assignmentError = signal<string | null>(null);
  indexing = signal(false);
  indexingError = signal<string | null>(null);
  indexingSuccess = signal<string | null>(null);
  photoStats = signal<{ indexed: number; unindexed: number }>({ indexed: 0, unindexed: 0 });

  eventId: string | null = null;
  photographers = signal<PhotographerSummary[]>([]);
  organizers = signal<EventOrganizerResponse[]>([]);

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
    organizerName: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    organizerId: this.fb.control<string | null>(''),
    registrationProvider: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    vectorCollectionId: this.fb.control('', { validators: [Validators.maxLength(160)] }),
    uploadPrefix: this.fb.control('', { validators: [Validators.maxLength(255)] }),
    timezone: this.fb.control('', { validators: [Validators.maxLength(60)] }),
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

  readonly assignmentForm = this.fb.group({
    slug: this.fb.control('', { validators: [Validators.maxLength(80)] }),
    email: this.fb.control('', { validators: [Validators.email, Validators.maxLength(160)] }),
    firstName: this.fb.control('', { validators: [Validators.maxLength(80)] }),
    lastName: this.fb.control('', { validators: [Validators.maxLength(80)] }),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly eventAdminService: EventAdminService,
    private readonly organizerAdminService: EventOrganizerAdminService,
  ) {}

  ngOnInit(): void {
    this.eventId = this.route.snapshot.paramMap.get('eventId');
    this.loadOrganizers();
    if (this.eventId) {
      this.fetchEvent(this.eventId);
    } else {
      this.loadError.set('Missing event id');
      this.loadingEvent.set(false);
    }
  }

  controlInvalid(path: string): boolean {
    const control = this.form.get(path);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  async fetchEvent(eventId: string): Promise<void> {
    this.loadingEvent.set(true);
    this.loadError.set(null);
    try {
      const event = await this.eventAdminService.getEvent(eventId);
      this.applyEvent(event);
    } catch (err) {
      console.error(err);
      this.loadError.set('Unable to load event.');
    } finally {
      this.loadingEvent.set(false);
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

  private applyEvent(event: EventDetail): void {
    this.photographers.set(event.photographers ?? []);
    this.photoStats.set({
      indexed: event.indexedPhotoCount ?? 0,
      unindexed: event.unindexedPhotoCount ?? 0,
    });
    this.form.patchValue({
      slug: event.slug,
      name: event.name,
      description: event.description,
      status: event.status,
      organizerName: event.organizerName,
      organizerId: event.organizer?.id ?? '',
      registrationProvider: event.registrationProvider,
      vectorCollectionId: event.vectorCollectionId,
      uploadPrefix: event.uploadPrefix,
      timezone: event.timezone,
      startTime: this.formatForInput(event.startTime),
      endTime: this.formatForInput(event.endTime),
      locationName: event.locationName,
      locationCity: event.locationCity,
      locationState: event.locationState,
      locationCountry: event.locationCountry,
      coverImageKey: event.coverImageKey,
      platformCommissionRate: event.platformCommissionRate,
      watermarkingEnabled: event.watermarkingEnabled,
      autoPublishMatches: event.autoPublishMatches,
      defaultPricing: {
        pricePerPhoto: event.defaultPricing.pricePerPhoto,
        bundlePrice: event.defaultPricing.bundlePrice,
        bundleSize: event.defaultPricing.bundleSize,
        currencyCode: event.defaultPricing.currencyCode,
      },
      accessPolicy: {
        mode: event.accessPolicy.mode,
        provider: event.accessPolicy.provider,
        configuration: event.accessPolicy.configuration,
      },
      participantMessage: event.participantMessage,
    });
  }

  async submit(): Promise<void> {
    if (!this.eventId) {
      this.submissionError.set('Missing event id');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submissionError.set(null);
    this.submissionSuccess.set(null);
    this.saving.set(true);
    const payload = this.buildPayload();

    try {
      const updated = await this.eventAdminService.updateEvent(this.eventId, payload);
      this.applyEvent(updated);
      this.submissionSuccess.set('Event updated successfully.');
    } catch (err) {
      console.error(err);
      this.submissionError.set('Unable to update event right now. Please retry.');
    } finally {
      this.saving.set(false);
    }
  }

  async assignPhotographer(): Promise<void> {
    if (!this.eventId) {
      this.assignmentError.set('Missing event id');
      return;
    }
    const raw = this.assignmentForm.getRawValue();
    const payload: AddPhotographerToEventRequest = {
      slug: this.cleanString(raw.slug),
      email: this.cleanString(raw.email),
      firstName: this.cleanString(raw.firstName),
      lastName: this.cleanString(raw.lastName),
    };

    if (!payload.slug && !payload.email && !(payload.firstName && payload.lastName)) {
      this.assignmentError.set('Provide slug, email, or first + last name.');
      return;
    }

    this.assignmentError.set(null);
    this.assigning.set(true);
    try {
      const updated = await this.eventAdminService.addPhotographerToEvent(this.eventId, payload);
      this.photographers.set(updated.photographers ?? []);
      this.assignmentForm.reset();
    } catch (err) {
      console.error(err);
      this.assignmentError.set('Unable to add photographer. Check the identifier and try again.');
    } finally {
      this.assigning.set(false);
    }
  }

  async indexFaces(): Promise<void> {
    if (!this.eventId) {
      return;
    }
    this.indexingError.set(null);
    this.indexingSuccess.set(null);
    this.indexing.set(true);
    try {
      await this.eventAdminService.indexFacesForEvent(this.eventId);
      const refreshed = await this.eventAdminService.getEvent(this.eventId);
      this.applyEvent(refreshed);
      this.indexingSuccess.set('Indexing started. Stats will refresh once complete.');
    } catch (err) {
      console.error(err);
      this.indexingError.set('Unable to start indexing right now.');
    } finally {
      this.indexing.set(false);
    }
  }

  async removePhotographer(photographer: PhotographerSummary): Promise<void> {
    if (!this.eventId) {
      return;
    }
    try {
      await this.eventAdminService.removePhotographerFromEvent(this.eventId, photographer.id);
      this.photographers.update(current => current.filter(p => p.id !== photographer.id));
    } catch (err) {
      console.error(err);
      this.assignmentError.set('Failed to remove photographer.');
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

  private formatForInput(value: string | null | undefined): string | null {
    if (!value) {
      return null;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
