import { Component, computed, inject, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { AppointmentStore } from '../appointment.store';
import { AppointmentStatus } from '../appointment.model';

@Component({
  selector: 'app-appointment-detail',
  standalone: true,
  imports: [DatePipe, ButtonModule, CardModule, TagModule, SkeletonModule],
  template: `
    <div class="appointment-detail">
      <nav class="appointment-detail__breadcrumb">
        <a (click)="router.navigate(['/appointments'])" class="appointment-detail__back">← Appointments</a>
      </nav>

      @if (store.loading() && !store.currentAppointment()) {
        <p-skeleton height="5rem" styleClass="mb-3" />
        <p-skeleton height="3rem" styleClass="mb-2" />
        <p-skeleton height="3rem" />
      }

      @if (store.currentAppointment(); as appt) {
        <p-card>
          <div class="appointment-detail__header">
            <h2 class="appointment-detail__title">Appointment Detail</h2>
            <p-tag [value]="appt.status" [severity]="statusSeverity(appt.status)" />
          </div>

          <div class="appointment-detail__fields">
            <div class="appointment-detail__field">
              <span class="appointment-detail__label">Patient</span>
              <span>{{ appt.patientName }}</span>
            </div>
            <div class="appointment-detail__field">
              <span class="appointment-detail__label">Doctor</span>
              <span>{{ appt.doctorName }}</span>
            </div>
            <div class="appointment-detail__field">
              <span class="appointment-detail__label">Scheduled</span>
              <span>{{ appt.scheduledAt | date:'medium' }}</span>
            </div>
            <div class="appointment-detail__field">
              <span class="appointment-detail__label">Reason</span>
              <span>{{ appt.reason }}</span>
            </div>
          </div>

          @if (actionLinks().length > 0) {
            <div class="appointment-detail__actions">
              @for (entry of actionLinks(); track entry[0]) {
                <p-button [label]="entry[0]" (click)="store.transition(entry[0])"></p-button>
              }
            </div>
          }
        </p-card>
      }
    </div>
  `,
  styles: [`
    .appointment-detail { max-width: 700px; }
    .appointment-detail__breadcrumb { margin-bottom: 1rem; font-size: 0.875rem; color: var(--p-text-muted-color); cursor: pointer; }
    .appointment-detail__back { color: var(--p-primary-color); text-decoration: none; }
    .appointment-detail__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
    .appointment-detail__title { margin: 0; font-size: 1.25rem; }
    .appointment-detail__fields { display: flex; flex-direction: column; gap: 0.75rem; }
    .appointment-detail__field { display: flex; gap: 1rem; }
    .appointment-detail__label { font-weight: 600; min-width: 100px; color: var(--p-text-muted-color); }
    .appointment-detail__actions { display: flex; gap: 0.75rem; margin-top: 1.5rem; flex-wrap: wrap; }
  `],
})
export class AppointmentDetailComponent implements OnInit {
  @Input() id!: string;

  protected readonly store = inject(AppointmentStore);
  protected readonly router = inject(Router);

  protected readonly actionLinks = computed(() => {
    const links = this.store.currentAppointment()?._links;
    if (!links) return [];
    return Object.entries(links).filter(([rel]) => rel !== 'self');
  });

  ngOnInit(): void {
    this.store.loadAppointment(this.id);
  }

  protected statusSeverity(status: AppointmentStatus): 'success' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case 'CONFIRMED': return 'success';
      case 'ARRIVED': return 'warn';
      case 'IN_PROGRESS': return 'warn';
      case 'CANCELLED': return 'danger';
      default: return 'secondary';
    }
  }
}
