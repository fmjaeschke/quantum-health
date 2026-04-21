import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { PatientStore } from '../patient.store';

@Component({
  selector: 'app-register-patient',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, DatePickerModule, ButtonModule, CardModule],
  template: `
    <div class="register-patient">
      <nav class="register-patient__breadcrumb">
        <a (click)="router.navigate(['/patients'])" class="register-patient__back">← Patients</a>
        <span> › Register New Patient</span>
      </nav>

      <h2>Register New Patient</h2>

      <p-card>
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="register-patient__form">
          <div class="register-patient__field">
            <label for="firstName">First name *</label>
            <input pInputText id="firstName" formControlName="firstName" placeholder="First name" class="w-full" />
            @if (form.controls.firstName.dirty && form.controls.firstName.hasError('required')) {
              <small class="register-patient__error">First name is required</small>
            }
          </div>

          <div class="register-patient__field">
            <label for="lastName">Last name *</label>
            <input pInputText id="lastName" formControlName="lastName" placeholder="Last name" class="w-full" />
            @if (form.controls.lastName.dirty && form.controls.lastName.hasError('required')) {
              <small class="register-patient__error">Last name is required</small>
            }
          </div>

          <div class="register-patient__field">
            <label for="dateOfBirth">Date of birth</label>
            <p-datepicker inputId="dateOfBirth" formControlName="dateOfBirth"
                          [showIcon]="true" dateFormat="dd/mm/yy" class="w-full" />
            @if (form.controls.dateOfBirth.dirty && form.controls.dateOfBirth.hasError('required')) {
              <small class="register-patient__error">Date of birth is required</small>
            }
          </div>

          <div class="register-patient__actions">
            <button pButton type="button" label="Cancel" severity="secondary"
                    (click)="router.navigate(['/patients'])"></button>
            <button pButton type="submit" label="Register Patient"
                    [disabled]="store.loading()"></button>
          </div>
        </form>
      </p-card>
    </div>
  `,
  styles: [`
    .register-patient { max-width: 600px; }
    .register-patient__breadcrumb { margin-bottom: 1rem; color: var(--p-text-muted-color); font-size: 0.875rem; cursor: pointer; }
    .register-patient__back { color: var(--p-primary-color); text-decoration: none; }
    .register-patient__form { display: flex; flex-direction: column; gap: 1.25rem; }
    .register-patient__field { display: flex; flex-direction: column; gap: 0.25rem; }
    .register-patient__error { color: var(--p-red-500); font-size: 0.8rem; }
    .register-patient__actions { display: flex; gap: 0.75rem; justify-content: flex-end; padding-top: 0.5rem; }
  `],
})
export class RegisterPatientComponent {
  protected readonly store = inject(PatientStore);
  protected readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    dateOfBirth: [null as Date | null, Validators.required],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      Object.values(this.form.controls).forEach(c => c.markAsDirty());
      return;
    }
    const dob = this.form.value.dateOfBirth!;
    const dateStr = [
      dob.getFullYear(),
      String(dob.getMonth() + 1).padStart(2, '0'),
      String(dob.getDate()).padStart(2, '0'),
    ].join('-');
    this.store.registerPatient({
      firstName: this.form.value.firstName!,
      lastName: this.form.value.lastName!,
      dateOfBirth: dateStr,
    });
  }
}
