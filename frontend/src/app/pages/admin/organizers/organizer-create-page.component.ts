import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EventOrganizerAdminService, EventOrganizerRequest, EventOrganizerStatus } from '../../../shared/event-organizer-admin.service';

interface OrganizerFormValue extends EventOrganizerRequest {}

@Component({
  selector: 'app-organizer-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styleUrls: ['./event-organizer-form-page.component.css'],
  templateUrl: './organizer-create-page.component.html',
})
export class OrganizerCreatePageComponent {
  readonly statuses: EventOrganizerStatus[] = ['ACTIVE', 'DISABLED'];

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
    name: this.fb.control('', { validators: [Validators.required, Validators.maxLength(160)] }),
    email: this.fb.control('', { validators: [Validators.required, Validators.email, Validators.maxLength(160)] }),
    phoneNumber: this.fb.control('', { validators: [Validators.maxLength(40)] }),
    status: this.fb.control<EventOrganizerStatus>('ACTIVE', { validators: [Validators.required] }),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly organizerAdminService: EventOrganizerAdminService,
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
      await this.organizerAdminService.createOrganizer(payload);
      this.submissionSuccess.set('Event organizer created successfully.');
      this.form.reset({
        status: 'ACTIVE',
      });
    } catch (err) {
      console.error(err);
      this.submissionError.set('Unable to create organizer right now. Please retry.');
    } finally {
      this.submitting.set(false);
    }
  }

  private buildPayload(): OrganizerFormValue {
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
