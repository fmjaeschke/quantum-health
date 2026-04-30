import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { APP_CONFIG } from '../core/app-config.token';
import { AppointmentService } from './appointment.service';

const mockConfig = { apiUrl: '/api', issuer: '', clientId: '', requireHttps: false };

const APPT_ID = 'appt-1';
const PATIENT_UUID = 'patient-1';
const TOMORROW = new Date(Date.now() + 86400000).toISOString().slice(0, 16);

const halAppointment = {
  id: APPT_ID,
  patientId: PATIENT_UUID,
  patientName: 'Alice Smith',
  doctorId: 'dr-smith',
  doctorName: 'Dr. Smith',
  scheduledAt: TOMORROW,
  status: 'SCHEDULED',
  _links: {
    self: { href: '/api/appointments/appt-1' },
    confirm: { href: '/api/appointments/appt-1/confirm' },
    cancel: { href: '/api/appointments/appt-1/cancel' },
  },
};

describe('AppointmentService', () => {
  let service: AppointmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: mockConfig },
      ],
    });
    service = TestBed.inject(AppointmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('schedule() sends POST /api/appointments and returns AppointmentResource', () => {
    let result: any;
    service.schedule({
      patientId: PATIENT_UUID,
      patientName: 'Alice Smith',
      doctorId: 'dr-smith',
      doctorName: 'Dr. Smith',
      scheduledAt: TOMORROW,
    }).subscribe(r => result = r);

    const req = httpMock.expectOne(r => r.url === '/api/appointments' && r.method === 'POST');
    req.flush(halAppointment);

    expect(result.id).toBe(APPT_ID);
    expect(result.patientName).toBe('Alice Smith');
    expect(result.status).toBe('SCHEDULED');
    expect(result._links?.confirm?.href).toBe('/api/appointments/appt-1/confirm');
  });

  it('get() sends GET /api/appointments/:id and returns AppointmentResource with links', () => {
    let result: any;
    service.get(APPT_ID).subscribe(r => result = r);

    httpMock.expectOne(r => r.url === `/api/appointments/${APPT_ID}`).flush(halAppointment);

    expect(result.id).toBe(APPT_ID);
    expect(result._links?.self?.href).toContain(APPT_ID);
  });

  it('list() sends GET /api/appointments and returns array', () => {
    let result: any;
    service.list().subscribe(r => result = r);

    httpMock.expectOne(r => r.url === '/api/appointments').flush({
      _embedded: { appointments: [halAppointment] },
      _links: { self: { href: '/api/appointments' } },
    });

    expect(result).toHaveLength(1);
    expect(result[0].patientName).toBe('Alice Smith');
  });

  it('list() returns [] when _embedded is absent', () => {
    let result: any;
    service.list().subscribe(r => result = r);
    httpMock.expectOne(r => r.url === '/api/appointments').flush({ _links: {} });
    expect(result).toEqual([]);
  });

  it('postTransition() sends POST to the given href', () => {
    let result: any;
    const href = '/api/appointments/appt-1/confirm';
    service.postTransition(href).subscribe(r => result = r);

    const req = httpMock.expectOne(r => r.url === href && r.method === 'POST');
    req.flush({ ...halAppointment, status: 'CONFIRMED', _links: { self: { href }, cancel: { href: '/api/appointments/appt-1/cancel' } } });

    expect(result.status).toBe('CONFIRMED');
  });
});
