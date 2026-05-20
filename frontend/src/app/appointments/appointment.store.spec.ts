import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AppointmentService } from './appointment.service';
import { AppointmentStore } from './appointment.store';

const APPT_ID = 'appt-1';
const mockAppointmentScheduled = {
  id: APPT_ID,
  patientId: 'p-1',
  patientName: 'Alice Smith',
  doctorId: 'dr-smith',
  doctorName: 'Dr. Smith',
  scheduledAt: '2026-05-01T10:00',
  reason: 'Routine checkup',
  status: 'PENDING' as const,
  _links: {
    self: { href: '/api/appointments/appt-1' },
    confirm: { href: '/api/appointments/appt-1/confirm' },
    cancel: { href: '/api/appointments/appt-1/cancel' },
  },
};
const mockConfirmed = {
  ...mockAppointmentScheduled,
  status: 'CONFIRMED' as const,
  _links: {
    self: { href: '/api/appointments/appt-1' },
    cancel: { href: '/api/appointments/appt-1/cancel' },
  },
};

const mockPage = {
  appointments: [mockAppointmentScheduled],
  page: 0,
  pageSize: 20,
  totalElements: 1,
};

describe('AppointmentStore', () => {
  const mockService = {
    schedule: vi.fn().mockReturnValue(of(mockAppointmentScheduled)),
    get: vi.fn().mockReturnValue(of(mockAppointmentScheduled)),
    list: vi.fn().mockReturnValue(of(mockPage)),
    postTransition: vi.fn().mockReturnValue(of(mockConfirmed)),
  };
  const mockRouter = { navigate: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    mockService.schedule.mockReturnValue(of(mockAppointmentScheduled));
    mockService.get.mockReturnValue(of(mockAppointmentScheduled));
    mockService.list.mockReturnValue(of(mockPage));
    mockService.postTransition.mockReturnValue(of(mockConfirmed));

    TestBed.configureTestingModule({
      providers: [
        { provide: AppointmentService, useValue: mockService },
        { provide: Router, useValue: mockRouter },
      ],
    });
  });

  it('starts with empty state', () => {
    const store = TestBed.inject(AppointmentStore);
    expect(store.appointments()).toEqual([]);
    expect(store.currentAppointment()).toBeNull();
    expect(store.loading()).toBe(false);
    expect(store.page()).toBe(0);
    expect(store.pageSize()).toBe(20);
    expect(store.statusFilter()).toBeNull();
  });

  it('loadAppointments() fetches list and stores it', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.loadAppointments();
    expect(mockService.list).toHaveBeenCalled();
    expect(store.appointments()).toHaveLength(1);
    expect(store.appointments()[0].patientName).toBe('Alice Smith');
    expect(store.loading()).toBe(false);
  });

  it('loadAppointments() passes current page, pageSize, and statusFilter to service', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.setStatusFilter('PENDING');
    mockService.list.mockClear();
    await store.loadAppointments();
    expect(mockService.list).toHaveBeenCalledWith({ status: 'PENDING', page: 0, pageSize: 20 });
  });

  it('setStatusFilter() sets filter, resets page to 0, and reloads', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.setStatusFilter('CONFIRMED');
    expect(store.statusFilter()).toBe('CONFIRMED');
    expect(store.page()).toBe(0);
    expect(mockService.list).toHaveBeenCalledTimes(1);
  });

  it('setStatusFilter(null) clears filter and reloads', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.setStatusFilter('PENDING');
    await store.setStatusFilter(null);
    expect(store.statusFilter()).toBeNull();
  });

  it('loadAppointment() fetches by id and stores it', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.loadAppointment(APPT_ID);
    expect(mockService.get).toHaveBeenCalledWith(APPT_ID);
    expect(store.currentAppointment()?.id).toBe(APPT_ID);
    expect(store.loading()).toBe(false);
  });

  it('scheduleAppointment() calls service and navigates to detail', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.scheduleAppointment({
      patientId: 'p-1',
      doctorId: 'dr-smith',
      scheduledAt: '2026-05-01T10:00',
      reason: 'Routine checkup',
    });
    expect(mockService.schedule).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/appointments', APPT_ID]);
  });

  it('transition() POSTs to HAL link href and patches currentAppointment without a secondary GET', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.loadAppointment(APPT_ID);
    await store.transition('confirm');
    expect(mockService.postTransition)
      .toHaveBeenCalledWith('/api/appointments/appt-1/confirm');
    expect(mockService.get).toHaveBeenCalledTimes(1);
    expect(store.currentAppointment()?.status).toBe('CONFIRMED');
  });

  it('transition() does nothing if link is not present', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.loadAppointment(APPT_ID);
    mockService.postTransition.mockClear();
    await store.transition('nonexistent');
    expect(mockService.postTransition).not.toHaveBeenCalled();
  });

  it('loadAppointments() sets loading false on error', async () => {
    mockService.list.mockReturnValue(throwError(() => new Error('network')));
    const store = TestBed.inject(AppointmentStore);
    await store.loadAppointments();
    expect(store.loading()).toBe(false);
  });

  it('loadAppointment() clears currentAppointment before fetching', async () => {
    const store = TestBed.inject(AppointmentStore);
    await store.loadAppointment(APPT_ID);
    expect(store.currentAppointment()?.id).toBe(APPT_ID);
    mockService.get.mockReturnValue(throwError(() => new Error('fail')));
    await store.loadAppointment('other-id');
    expect(store.currentAppointment()).toBeNull();
  });
});
