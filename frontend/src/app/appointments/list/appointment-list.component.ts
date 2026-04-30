import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { AppointmentStore } from '../appointment.store';
import { AppointmentStatus } from '../appointment.model';

@Component({
  selector: 'app-appointment-list',
  standalone: true,
  imports: [RouterLink, DatePipe, ButtonModule, TagModule, SkeletonModule],
  template: `
    <div class="appointment-list">
      <div class="appointment-list__header">
        <h2>Appointments</h2>
        <p-button label="Schedule New" icon="pi pi-plus"
                  (click)="router.navigate(['/appointments', 'new'])"></p-button>
      </div>

      <table class="appointment-list__table">
        <thead>
        <tr>
          <th>Patient</th>
          <th>Doctor</th>
          <th>Date &amp; Time</th>
          <th>Status</th>
          <th></th>
        </tr>
        </thead>
        <tbody>
          @if (store.loading()) {
            @for (_ of [1, 2, 3]; track $index) {
              <tr>
                <td colspan="5">
                  <p-skeleton height="1.5rem"/>
                </td>
              </tr>
            }
          } @else {
            @for (appointment of store.appointments(); track appointment.id) {
              <tr>
                <td>{{ appointment.patientName }}</td>
                <td>{{ appointment.doctorName }}</td>
                <td>{{ appointment.scheduledAt | date:'short' }}</td>
                <td>
                  <p-tag [value]="appointment.status" [severity]="statusSeverity(appointment.status)"/>
                </td>
                <td class="appointment-list__actions">
                  <p-button [routerLink]="['/appointments', appointment.id]" label="View" size="small"
                          severity="secondary" icon="pi pi-eye"></p-button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="appointment-list__empty">No appointments found.</td>
              </tr>
            }
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .appointment-list { display: flex; flex-direction: column; gap: 1rem; }
    .appointment-list__header { display: flex; justify-content: space-between; align-items: center; }
    .appointment-list__table { width: 100%; border-collapse: collapse; }
    .appointment-list__table th { text-align: left; padding: 0.5rem 0.75rem; background: var(--p-surface-100); }
    .appointment-list__table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid var(--p-surface-200); }
    .appointment-list__actions { text-align: center; }
    .appointment-list__empty { text-align: center; color: var(--p-text-muted-color); padding: 2rem; }
  `],
})
export class AppointmentListComponent implements OnInit {
  protected readonly store = inject(AppointmentStore);
  protected readonly router = inject(Router);

  ngOnInit(): void {
    this.store.loadAppointments();
  }

  protected statusSeverity(status: AppointmentStatus): 'success' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case 'CONFIRMED': return 'success';
      case 'SCHEDULED': return 'warn';
      case 'CANCELLED': return 'danger';
      default: return 'secondary';
    }
  }
}
