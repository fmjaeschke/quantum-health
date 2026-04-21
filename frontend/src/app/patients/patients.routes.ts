import { Routes } from '@angular/router';
import { PatientListComponent } from './list/patient-list.component';
import { RegisterPatientComponent } from './register/register-patient.component';
import { PatientDetailComponent } from './detail/patient-detail.component';

export const patientsRoutes: Routes = [
  { path: '', component: PatientListComponent },
  { path: 'new', component: RegisterPatientComponent },
  { path: ':id', component: PatientDetailComponent },
];
