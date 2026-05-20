import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { AutoCompleteModule, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { AppointmentStore } from '../appointment.store';
import { DoctorService } from '../doctor.service';
import { Doctor } from '../appointment.model';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-schedule-appointment',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, DatePickerModule, AutoCompleteModule],
  template: `
    <div class="schedule-appointment">
      <nav class="schedule-appointment__breadcrumb">
        <a (click)="router.navigate(['/appointments'])" class="schedule-appointment__back">← Appointments</a>
        <span> › Schedule New Appointment</span>
      </nav>

      <h2>Schedule New Appointment</h2>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="schedule-appointment__form">
        <div class="schedule-appointment__field">
          <label for="patientId">Patient ID *</label>
          <input pInputText id="patientId" formControlName="patientId" placeholder="Patient ID" class="w-full" />
          @if (form.controls.patientId.dirty && form.controls.patientId.hasError('required')) {
            <small class="schedule-appointment__error">Patient ID is required</small>
          }
        </div>

        <div class="schedule-appointment__field">
          <label for="doctor">Doctor *</label>
          <p-autocomplete
            inputId="doctor"
            [suggestions]="filteredDoctors()"
            field="displayName"
            (completeMethod)="filterDoctors($event.query)"
            (onSelect)="onDoctorSelect($event)"
            [forceSelection]="true"
            placeholder="Search doctor..."
            class="w-full"
          />
          @if (form.controls.doctorId.dirty && form.controls.doctorId.hasError('required')) {
            <small class="schedule-appointment__error">Doctor is required</small>
          }
        </div>

        <div class="schedule-appointment__field">
          <label for="scheduledAt">Date &amp; Time *</label>
          <p-datepicker inputId="scheduledAt" formControlName="scheduledAt"
                        [showTime]="true" [showIcon]="true" dateFormat="dd/mm/yy" class="w-full" />
          @if (form.controls.scheduledAt.dirty && form.controls.scheduledAt.hasError('required')) {
            <small class="schedule-appointment__error">Date &amp; Time is required</small>
          }
        </div>

        <div class="schedule-appointment__field">
          <label for="reason">Reason *</label>
          <input pInputText id="reason" formControlName="reason" placeholder="Reason for visit" class="w-full" />
          @if (form.controls.reason.dirty && form.controls.reason.hasError('required')) {
            <small class="schedule-appointment__error">Reason is required</small>
          }
        </div>

        <div class="schedule-appointment__actions">
          <p-button type="button" label="Cancel" severity="secondary"
                  (click)="router.navigate(['/appointments'])"></p-button>
          <p-button type="submit" label="Schedule Appointment"
                  [disabled]="form.invalid || store.loading()"></p-button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .schedule-appointment { max-width: 600px; }
    .schedule-appointment__breadcrumb { margin-bottom: 1rem; color: var(--p-text-muted-color); font-size: 0.875rem; }
    .schedule-appointment__back { color: var(--p-primary-color); text-decoration: none; cursor: pointer; }
    .schedule-appointment__form { display: flex; flex-direction: column; gap: 1.25rem; }
    .schedule-appointment__field { display: flex; flex-direction: column; gap: 0.25rem; }
    .schedule-appointment__error { color: var(--p-red-500); font-size: 0.8rem; }
    .schedule-appointment__actions { display: flex; gap: 0.75rem; justify-content: flex-end; padding-top: 0.5rem; }
  `],
})
export class ScheduleAppointmentComponent implements OnInit {
  protected readonly store = inject(AppointmentStore);
  protected readonly router = inject(Router);
  private readonly doctorService = inject(DoctorService);
  private readonly fb = inject(FormBuilder);

  private allDoctors: Doctor[] = [];
  protected readonly filteredDoctors = signal<Doctor[]>([]);

  protected readonly form = this.fb.group({
    patientId: ['', Validators.required],
    doctorId: ['', Validators.required],
    scheduledAt: [null as Date | null, Validators.required],
    reason: ['', Validators.required],
  });

  async ngOnInit(): Promise<void> {
    try {
      this.allDoctors = await firstValueFrom(this.doctorService.list());
    } catch {
      this.allDoctors = [];
    }
  }

  protected filterDoctors(query: string): void {
    const lower = query.toLowerCase();
    this.filteredDoctors.set(
      this.allDoctors.filter(d => d.displayName.toLowerCase().includes(lower))
    );
  }

  protected onDoctorSelect(event: AutoCompleteSelectEvent): void {
    const doctor = event.value as Doctor;
    this.form.controls.doctorId.setValue(doctor.id);
  }

  protected async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      Object.values(this.form.controls).forEach(c => c.markAsDirty());
      return;
    }
    const { patientId, doctorId, scheduledAt, reason } = this.form.getRawValue();
    const date = scheduledAt as Date;
    const dateStr = [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
    const timeStr = [
      String(date.getHours()).padStart(2, '0'),
      String(date.getMinutes()).padStart(2, '0'),
    ].join(':');
    await this.store.scheduleAppointment({
      patientId: patientId!,
      doctorId: doctorId!,
      scheduledAt: `${dateStr}T${timeStr}`,
      reason: reason!,
    });
  }
}
