import { Component, inject, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { PatientStore } from '../patient.store';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [DatePipe, CardModule, SkeletonModule],
  template: `
    <div class="patient-detail">
      <nav class="patient-detail__breadcrumb">
        <a (click)="router.navigate(['/patients'])" class="patient-detail__back">← Patients</a>
      </nav>

      @if (store.loading() && !store.currentPatient()) {
        <p-skeleton height="5rem" styleClass="mb-3" />
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem">
          <p-skeleton height="6rem" />
          <p-skeleton height="6rem" />
        </div>
      }

      @if (store.currentPatient(); as patient) {
        <p-card styleClass="patient-detail__header-card">
          <div class="patient-detail__header">
            <div class="patient-detail__avatar">
              {{ patient.firstName[0].toUpperCase() }}{{ patient.lastName[0].toUpperCase() }}
            </div>
            <div>
              <h2 class="patient-detail__name">{{ patient.firstName }} {{ patient.lastName }}</h2>
              <p class="patient-detail__dob">Born {{ patient.dateOfBirth | date:'d MMMM y' }}</p>
            </div>
          </div>
        </p-card>

        <div class="patient-detail__sections">
          <p-card styleClass="patient-detail__placeholder-card">
            <h3>Appointments</h3>
            <p class="patient-detail__placeholder-text">Coming in a future sprint</p>
          </p-card>
          <p-card styleClass="patient-detail__placeholder-card">
            <h3>Encounters</h3>
            <p class="patient-detail__placeholder-text">Coming in a future sprint</p>
          </p-card>
        </div>
      }
    </div>
  `,
  styles: [`
    .patient-detail { display: flex; flex-direction: column; gap: 1.5rem; max-width: 900px; }
    .patient-detail__breadcrumb { font-size: 0.875rem; color: var(--p-text-muted-color); cursor: pointer; }
    .patient-detail__back { color: var(--p-primary-color); text-decoration: none; }
    .patient-detail__header { display: flex; align-items: center; gap: 1rem; }
    .patient-detail__avatar {
      width: 56px; height: 56px; border-radius: 50%;
      background: var(--p-primary-100); color: var(--p-primary-700);
      display: flex; align-items: center; justify-content: center;
      font-size: 1.25rem; font-weight: 700;
    }
    .patient-detail__name { margin: 0; font-size: 1.5rem; }
    .patient-detail__dob { margin: 0.25rem 0 0; color: var(--p-text-muted-color); }
    .patient-detail__sections { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .patient-detail__placeholder-card { opacity: 0.5; }
    .patient-detail__placeholder-text { color: var(--p-text-muted-color); font-style: italic; }
  `],
})
export class PatientDetailComponent implements OnInit {
  @Input() id!: string;

  protected readonly store = inject(PatientStore);
  protected readonly router = inject(Router);

  ngOnInit(): void {
    this.store.loadPatient(this.id);
  }
}
