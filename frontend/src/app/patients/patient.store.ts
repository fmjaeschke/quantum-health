import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { Patient, PatientPage, RegisterPatientRequest, SortDirection, SortField } from './patient.model';
import { PatientService } from './patient.service';

interface PatientState {
  patients: Patient[];
  currentPatient: Patient | null;
  totalElements: number;
  totalPages: number;
  page: number;
  pageSize: number;
  search: string;
  sortField: SortField;
  sortDirection: SortDirection;
  loading: boolean;
}

const initialState: PatientState = {
  patients: [],
  currentPatient: null,
  totalElements: 0,
  totalPages: 0,
  page: 0,
  pageSize: 20,
  search: '',
  sortField: 'LAST_NAME',
  sortDirection: 'ASC',
  loading: false,
};

export const PatientStore = signalStore(
  { providedIn: 'root' },
  withState<PatientState>(initialState),
  withMethods((store, patientService = inject(PatientService), router = inject(Router)) => {
    async function loadPatients(): Promise<void> {
      patchState(store, { loading: true });
      try {
        const params = {
          page: store.page(),
          size: store.pageSize(),
          sort: store.sortField(),
          direction: store.sortDirection(),
          ...(store.search() ? { search: store.search() } : {}),
        };
        const result: PatientPage = await firstValueFrom(patientService.list(params));
        patchState(store, {
          patients: result.patients,
          totalElements: result.totalElements,
          totalPages: result.totalPages,
          loading: false,
        });
      } catch {
        patchState(store, { loading: false });
      }
    }

    return {
      loadPatients,

      async setSearch(term: string): Promise<void> {
        patchState(store, { search: term, page: 0 });
        await loadPatients();
      },

      async setSort(field: SortField): Promise<void> {
        const direction: SortDirection =
          store.sortField() === field && store.sortDirection() === 'ASC' ? 'DESC' : 'ASC';
        patchState(store, { sortField: field, sortDirection: direction, page: 0 });
        await loadPatients();
      },

      async goToPage(n: number): Promise<void> {
        patchState(store, { page: n });
        await loadPatients();
      },

      async loadPatient(id: string): Promise<void> {
        patchState(store, { loading: true, currentPatient: null });
        try {
          const patient = await firstValueFrom(patientService.get(id));
          patchState(store, { currentPatient: patient, loading: false });
        } catch {
          patchState(store, { loading: false });
        }
      },

      async registerPatient(req: RegisterPatientRequest): Promise<void> {
        patchState(store, { loading: true });
        try {
          const patient = await firstValueFrom(patientService.register(req));
          patchState(store, { loading: false });
          await router.navigate(['/patients', patient.id]);
        } catch {
          patchState(store, { loading: false });
        }
      },
    };
  })
);
