import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { PrescriptionResource } from './prescription.model';
import { PrescriptionService } from './prescription.service';

interface PrescriptionState {
  prescriptions: PrescriptionResource[];
  currentPrescription: PrescriptionResource | null;
  loading: boolean;
}

const initialState: PrescriptionState = {
  prescriptions: [],
  currentPrescription: null,
  loading: false,
};

export const PrescriptionStore = signalStore(
  { providedIn: 'root' },
  withState<PrescriptionState>(initialState),
  withMethods((store, service = inject(PrescriptionService)) => {
    function applyUpdatedPrescription(updated: PrescriptionResource): void {
      patchState(store, {
        currentPrescription: store.currentPrescription()?.id === updated.id
          ? updated
          : store.currentPrescription(),
        prescriptions: store.prescriptions().map((p) => (p.id === updated.id ? updated : p)),
      });
    }

    return {
      async loadPrescriptions(page = 0, size = 20): Promise<void> {
        patchState(store, { loading: true });
        try {
          const response = await firstValueFrom(service.list(page, size));
          patchState(store, { prescriptions: response._embedded.prescriptions, loading: false });
        } catch {
          patchState(store, { loading: false });
        }
      },

      async loadPrescription(id: string): Promise<void> {
        patchState(store, { loading: true, currentPrescription: null });
        try {
          const prescription = await firstValueFrom(service.get(id));
          patchState(store, { currentPrescription: prescription, loading: false });
        } catch {
          patchState(store, { loading: false });
        }
      },

      async fulfill(prescription: PrescriptionResource): Promise<void> {
        const href = prescription._links?.['fulfill-prescription']?.href;
        if (!href) return;

        patchState(store, { loading: true });
        try {
          const updated = await firstValueFrom(service.postAction(href));
          applyUpdatedPrescription(updated);
          patchState(store, { loading: false });
        } catch {
          patchState(store, { loading: false });
        }
      },

      async cancel(prescription: PrescriptionResource, reason: string): Promise<boolean> {
        const href = prescription._links?.['cancel-prescription']?.href;
        if (!href) return false;

        patchState(store, { loading: true });
        try {
          const updated = await firstValueFrom(service.postActionWithBody(href, { reason }));
          applyUpdatedPrescription(updated);
          patchState(store, { loading: false });
          return true;
        } catch {
          patchState(store, { loading: false });
          return false;
        }
      },
    };
  })
);
