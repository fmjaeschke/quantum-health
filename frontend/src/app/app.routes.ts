import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';
import { ShellComponent } from './shell/shell.component';

export const appRoutes: Routes = [
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'patients',
        loadChildren: () =>
          import('./patients/patients.routes').then(m => m.patientsRoutes),
      },
      {
        path: 'appointments',
        loadChildren: () =>
          import('./appointments/appointments.routes').then(m => m.appointmentsRoutes),
      },
      {
        path: 'encounters',
        loadChildren: () =>
          import('./encounters/encounters.routes').then(m => m.encountersRoutes),
      },
      {
        path: 'pharmacy',
        canActivate: [roleGuard('PHARMACIST')],
        loadChildren: () =>
          import('./pharmacy/pharmacy.routes').then(m => m.pharmacyRoutes),
      },
      {
        path: 'billing',
        canActivate: [roleGuard('BILLING_CLERK')],
        loadChildren: () =>
          import('./billing/billing.routes').then(m => m.billingRoutes),
      },
      {
        path: 'lab',
        loadChildren: () =>
          import('./lab/lab.routes').then(m => m.labRoutes),
      },
      {
        path: 'audit',
        canActivate: [roleGuard('ADMIN')],
        loadChildren: () =>
          import('./audit/audit.routes').then(m => m.auditRoutes),
      },
      { path: '', redirectTo: 'patients', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
