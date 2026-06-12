import { Routes } from '@angular/router';

export const pharmacyRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pharmacist-dashboard/pharmacist-dashboard.component').then(
        m => m.PharmacistDashboardComponent
      ),
  },
];
