import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { errorInterceptor } from './error.interceptor';
import { AuthStore } from '../../auth/auth.store';
import { GlobalErrorService } from '../global-error.service';
import { APP_CONFIG } from '../app-config.token';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let mockAuthStore: { token: ReturnType<typeof signal<string | null>>; roles: ReturnType<typeof signal<string[]>>; login: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn>; configure: ReturnType<typeof vi.fn> };
  let errorService: GlobalErrorService;

  beforeEach(() => {
    mockAuthStore = {
      token: signal<string | null>(null),
      roles: signal<string[]>([]),
      login: vi.fn(),
      logout: vi.fn(),
      configure: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthStore, useValue: mockAuthStore },
        { provide: APP_CONFIG, useValue: { apiUrl: '/api', issuer: '', clientId: '', requireHttps: false } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    errorService = TestBed.inject(GlobalErrorService);
  });

  afterEach(() => controller.verify());

  it('calls logout on 401', () => {
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({ detail: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    expect(mockAuthStore.logout).toHaveBeenCalled();
  });

  it('shows "Access denied" error on 403 with RFC 7807 detail', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({ detail: 'Insufficient permissions' }, { status: 403, statusText: 'Forbidden' });
    expect(showSpy).toHaveBeenCalledWith('Access denied', 'Insufficient permissions');
  });

  it('shows generic error on 500 with RFC 7807 detail', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({ detail: 'Unexpected server error' }, { status: 500, statusText: 'Internal Server Error' });
    expect(showSpy).toHaveBeenCalledWith('Error', 'Unexpected server error');
  });

  it('passes non-API requests through without error handling', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('http://localhost:8180/realms/test/certs').subscribe({ error: () => {} });
    controller
      .expectOne('http://localhost:8180/realms/test/certs')
      .flush({}, { status: 503, statusText: 'Service Unavailable' });
    expect(showSpy).not.toHaveBeenCalled();
    expect(mockAuthStore.logout).not.toHaveBeenCalled();
  });

  it('falls back to message when RFC 7807 detail is absent', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({}, { status: 503, statusText: 'Service Unavailable' });
    expect(showSpy).toHaveBeenCalledWith('Error', 'Http failure response for /api/test: 503 Service Unavailable');
  });

  it('shows generic error on 4xx other than 401/403 (e.g. 404)', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({ detail: 'Patient not found' }, { status: 404, statusText: 'Not Found' });
    expect(showSpy).toHaveBeenCalledWith('Error', 'Patient not found');
  });

  it('falls back to message on 403 when RFC 7807 detail is absent', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({}, { status: 403, statusText: 'Forbidden' });
    expect(showSpy).toHaveBeenCalledWith('Access denied', 'Http failure response for /api/test: 403 Forbidden');
  });

  it('shows generic error, not "Access denied", when a read is hidden as 404 for a non-owner', () => {
    // Backend converges Appointment/Patient/Prescription reads on 404 for non-owner doctors
    // instead of 403, so existence is never leaked. This asserts the interceptor's generic-4xx
    // branch (not its 403 branch) fires for that response. See issues/034.
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/appointments/123').subscribe({ error: () => {} });
    controller
      .expectOne('/api/appointments/123')
      .flush({ detail: 'Appointment not found' }, { status: 404, statusText: 'Not Found' });
    expect(showSpy).toHaveBeenCalledWith('Error', 'Appointment not found');
  });

  it('does not call show() on 401', () => {
    const showSpy = vi.spyOn(errorService, 'show');
    http.get('/api/test').subscribe({ error: () => {} });
    controller
      .expectOne('/api/test')
      .flush({ detail: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    expect(showSpy).not.toHaveBeenCalled();
  });
});
