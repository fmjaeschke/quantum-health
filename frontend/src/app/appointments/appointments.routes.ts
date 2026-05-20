import { Routes } from '@angular/router';
import { AppointmentListComponent } from './list/appointment-list.component';
import { ScheduleAppointmentComponent } from './schedule/schedule-appointment.component';
import { AppointmentDetailComponent } from './detail/appointment-detail.component';

export const appointmentsRoutes: Routes = [
  { path: '', component: AppointmentListComponent },
  { path: 'new', component: ScheduleAppointmentComponent },
  { path: ':id', component: AppointmentDetailComponent },
];
