import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EventOrganizerAdminService, EventOrganizerRequest, EventOrganizerResponse, EventOrganizerStatus } from '../../../shared/event-organizer-admin.service';

@Component({
  selector: 'app-organizer-edit-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styleUrls: ['./event-organizer-form-page.component.css'],
  templateUrl: './organizer-edit-page.component.html',
})
export class OrganizerEditPageComponent implements OnInit {
  readonly statuses: EventOrganizerStatus[] = ['ACTIVE', 'DISABLED'];

  loading = signal(true);
  submitting = signal(false);
  submissionError = signal<string | null>(null);
  submissionSuccess = signal<string | null>(null);
  disableError = signal<string | null>(null);

  organizerId: string | null = null;

  readonly form = this.fb.group({
    slug: this.fb.control('', {
      validators: [
        Validators.required,
        Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
        Validators.maxLength(80),
      ],
    }),
    name: this.fb.control('', { validators: [Validators.required, Validators.maxLength(160)] }),
    email: this.fb.control('', { validators: [Validators.required, Validators.email, Validators.maxLength(160)] }),
    phoneNumber: this.fb.control('', { validators: [Validators.maxLength(40)] }),
    status: this.fb.control<EventOrganizerStatus>('ACTIVE', { validators: [Validators.required] }),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly organizerAdminService: EventOrganizerAdminService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  controlInvalid(path: string): boolean {
    const control = this.form.get(path);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  async load(): Promise<void> {
    const organizerId = this.route.snapshot.paramMap.get('organizerId');
    if (!organizerId) {
      this.submissionError.set('Organizer id is missing from the route.');
      this.loading.set(false);
      return;
    }
    this.organizerId = organizerId;

    this.loading.set(true);
    this.submissionError.set(null);
    try {
      const organizer = await this.organizerAdminService.getOrganizer(organizerId);
      this.patchForm(organizer);
    } catch (err) {
      console.error(err);
      this.submissionError.set('Unable to load organizer. Please refresh.');
    } finally {
      this.loading.set(false);
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || !this.organizerId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.submissionError.set(null);
    this.submissionSuccess.set(null);
    const payload = this.buildPayload();

    try {
      const updated = await this.organizerAdminService.updateOrganizer(this.organizerId, payload);
      this.patchForm(updated);
      this.submissionSuccess.set('Organizer updated.');
    } catch (err) {
      console.error(err);
      this.submissionError.set('Unable to update organizer right now.');
    } finally {
      this.submitting.set(false);
    }
  }

  async disable(): Promise<void> {
    if (!this.organizerId) {
      return;
    }
    this.disableError.set(null);
    try {
      await this.organizerAdminService.disableOrganizer(this.organizerId);
      this.form.patchValue({ status: 'DISABLED' });
      this.submissionSuccess.set('Organizer disabled.');
    } catch (err) {
      console.error(err);
      this.disableError.set('Unable to disable organizer.');
    }
  }

  private patchForm(organizer: EventOrganizerResponse): void {
    this.form.patchValue({
      slug: organizer.slug,
      name: organizer.name,
      email: organizer.email,
      phoneNumber: organizer.phoneNumber ?? '',
      status: organizer.status,
    });
  }

  private buildPayload(): EventOrganizerRequest {
    const raw = this.form.getRawValue();
    return {
      slug: raw.slug!.trim(),
      name: raw.name!.trim(),
      email: raw.email!.trim(),
      phoneNumber: this.cleanString(raw.phoneNumber),
      status: raw.status ?? 'ACTIVE',
    };
  }

  private cleanString(value: unknown): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    const str = String(value).trim();
    return str.length ? str : null;
  }
}
