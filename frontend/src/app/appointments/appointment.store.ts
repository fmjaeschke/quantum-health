import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { AppointmentResource, ScheduleAppointmentRequest } from './appointment.model';
import { AppointmentService } from './appointment.service';

interface AppointmentState {
  appointments: AppointmentResource[];
  currentAppointment: AppointmentResource | null;
  loading: boolean;
}

const initialState: AppointmentState = {
  appointments: [],
  currentAppointment: null,
  loading: false,
};

export const AppointmentStore = signalStore(
  { providedIn: 'root' },
  withState<AppointmentState>(initialState),
  withMethods((store, service = inject(AppointmentService), router = inject(Router)) => ({
    async loadAppointments(): Promise<void> {
      patchState(store, { loading: true });
      try {
        const appointments = await firstValueFrom(service.list());
        patchState(store, { appointments, loading: false });
      } catch {
        patchState(store, { loading: false });
      }
    },

    async loadAppointment(id: string): Promise<void> {
      patchState(store, { loading: true, currentAppointment: null });
      try {
        const appointment = await firstValueFrom(service.get(id));
        patchState(store, { currentAppointment: appointment, loading: false });
      } catch {
        patchState(store, { loading: false });
      }
    },

    async scheduleAppointment(req: ScheduleAppointmentRequest): Promise<void> {
      patchState(store, { loading: true });
      try {
        const appointment = await firstValueFrom(service.schedule(req));
        patchState(store, { loading: false });
        await router.navigate(['/appointments', appointment.id]);
      } catch {
        patchState(store, { loading: false });
      }
    },

    async transition(rel: string): Promise<void> {
      const current = store.currentAppointment();
      // rel is a data-driven HAL link name; keyof cast is intentional
      const href = current?._links?.[rel as keyof NonNullable<typeof current._links>]?.href;
      if (!href) return;

      patchState(store, { loading: true });
      try {
        const updated = await firstValueFrom(service.postTransition(href));
        patchState(store, { currentAppointment: updated, loading: false });
        service.get(updated.id).subscribe({ error: () => {} }); // best-effort reload, ignore errors
      } catch {
        patchState(store, { loading: false });
      }
    },
  }))
);
