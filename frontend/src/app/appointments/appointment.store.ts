import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { AppointmentResource, AppointmentStatus, ScheduleAppointmentRequest } from './appointment.model';
import { AppointmentService } from './appointment.service';

interface AppointmentState {
  appointments: AppointmentResource[];
  currentAppointment: AppointmentResource | null;
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  statusFilter: AppointmentStatus | null;
}

const initialState: AppointmentState = {
  appointments: [],
  currentAppointment: null,
  loading: false,
  page: 0,
  pageSize: 20,
  totalElements: 0,
  statusFilter: null,
};

export const AppointmentStore = signalStore(
  { providedIn: 'root' },
  withState<AppointmentState>(initialState),
  withMethods((store, service = inject(AppointmentService), router = inject(Router)) => ({
    async loadAppointments(): Promise<void> {
      patchState(store, { loading: true });
      try {
        const result = await firstValueFrom(service.list({
          status: store.statusFilter() ?? undefined,
          page: store.page(),
          pageSize: store.pageSize(),
        }));
        patchState(store, {
          appointments: result.appointments,
          totalElements: result.totalElements,
          loading: false,
        });
      } catch {
        patchState(store, { loading: false });
      }
    },

    async setStatusFilter(status: AppointmentStatus | null): Promise<void> {
      patchState(store, { statusFilter: status, page: 0 });
      await this.loadAppointments();
    },

    async setPage(page: number, pageSize: number): Promise<void> {
      patchState(store, { page, pageSize });
      await this.loadAppointments();
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
      } catch {
        patchState(store, { loading: false });
      }
    },
  }))
);
