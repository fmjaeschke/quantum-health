import { render, screen, fireEvent } from '@testing-library/angular';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { AppointmentDetailComponent } from './appointment-detail.component';
import { AppointmentStore } from '../appointment.store';

const mockAppt = {
  id: 'a-1',
  patientId: 'p-1',
  patientName: 'Alice Smith',
  doctorId: 'dr-1',
  doctorName: 'Dr. Smith',
  scheduledAt: '2026-05-01T10:00:00Z',
  reason: 'Annual checkup',
  status: 'PENDING' as const,
  _links: {
    self: { href: '/api/appointments/a-1' },
    confirm: { href: '/api/appointments/a-1/confirm' },
    cancel: { href: '/api/appointments/a-1/cancel' },
  },
};

function makeStore(overrides: Record<string, unknown> = {}) {
  return {
    currentAppointment: signal(mockAppt as typeof mockAppt | null),
    loading: signal(false),
    loadAppointment: vi.fn().mockResolvedValue(undefined),
    transition: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

const providers = (store: ReturnType<typeof makeStore>) => [
  { provide: AppointmentStore, useValue: store },
  { provide: Router, useValue: { navigate: vi.fn() } },
];

describe('AppointmentDetailComponent', () => {
  it('calls loadAppointment with route id on init', async () => {
    const store = makeStore();
    await render(AppointmentDetailComponent, {
      componentInputs: { id: 'a-1' },
      providers: providers(store),
    });
    expect(store.loadAppointment).toHaveBeenCalledWith('a-1');
  });

  it('renders a button for each non-self link', async () => {
    const store = makeStore();
    await render(AppointmentDetailComponent, {
      componentInputs: { id: 'a-1' },
      providers: providers(store),
    });
    const buttons = screen.getAllByRole('button');
    const labels = buttons.map(b => b.textContent?.trim()).filter(Boolean);
    expect(labels).toHaveLength(2);
    expect(labels).toContain('Confirm');
    expect(labels).toContain('Cancel');
  });

  it('uses mapped labels: confirm → Confirm, check-in → Check In, start → Start Encounter, cancel → Cancel', async () => {
    const store = makeStore({
      currentAppointment: signal({
        ...mockAppt,
        _links: {
          self: { href: '/api/appointments/a-1' },
          confirm: { href: '/api/appointments/a-1/confirm' },
          'check-in': { href: '/api/appointments/a-1/check-in' },
          start: { href: '/api/appointments/a-1/start' },
          cancel: { href: '/api/appointments/a-1/cancel' },
        },
      }),
    });
    await render(AppointmentDetailComponent, {
      componentInputs: { id: 'a-1' },
      providers: providers(store),
    });
    expect(screen.getByRole('button', { name: 'Confirm' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Check In' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Start Encounter' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeTruthy();
  });

  it('falls back to capitalised rel for unknown rels', async () => {
    const store = makeStore({
      currentAppointment: signal({
        ...mockAppt,
        _links: {
          self: { href: '/api/appointments/a-1' },
          archive: { href: '/api/appointments/a-1/archive' },
        } as unknown as typeof mockAppt._links,
      }),
    });
    await render(AppointmentDetailComponent, {
      componentInputs: { id: 'a-1' },
      providers: providers(store),
    });
    expect(screen.getByRole('button', { name: 'Archive' })).toBeTruthy();
  });

  it('calls transition with the rel when a button is clicked', async () => {
    const store = makeStore();
    await render(AppointmentDetailComponent, {
      componentInputs: { id: 'a-1' },
      providers: providers(store),
    });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    expect(store.transition).toHaveBeenCalledWith('confirm');
  });

  it('renders no buttons when appointment has no non-self links', async () => {
    const store = makeStore({
      currentAppointment: signal({
        ...mockAppt,
        _links: { self: { href: '/api/appointments/a-1' } },
      }),
    });
    await render(AppointmentDetailComponent, {
      componentInputs: { id: 'a-1' },
      providers: providers(store),
    });
    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });
});
