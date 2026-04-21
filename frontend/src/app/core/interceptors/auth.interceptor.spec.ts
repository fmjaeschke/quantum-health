import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { authInterceptor } from './auth.interceptor';
import { APP_CONFIG, AppConfig } from '../app-config.token';
import { AuthStore } from '../../auth/auth.store';

const mockConfig: AppConfig = {
  apiUrl: '/api',
  issuer: '',
  clientId: '',
  requireHttps: false,
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  function setup(token: string | null) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: mockConfig },
        {
          provide: AuthStore,
          useValue: { token: signal(token), roles: signal([]), login: vi.fn(), logout: vi.fn(), configure: vi.fn() },
        },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  }

  afterEach(() => controller.verify());

  it('adds Authorization header for API requests when token is present', () => {
    setup('test-token');
    http.get('/api/patients').subscribe();
    const req = controller.expectOne('/api/patients');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({});
  });

  it('does not add Authorization header when token is null', () => {
    setup(null);
    http.get('/api/patients').subscribe();
    const req = controller.expectOne('/api/patients');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does not add Authorization header for requests not targeting apiUrl', () => {
    setup('test-token');
    http.get('https://external.example.com/data').subscribe();
    const req = controller.expectOne('https://external.example.com/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
