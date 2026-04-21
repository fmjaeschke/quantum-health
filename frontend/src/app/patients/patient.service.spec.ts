import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { APP_CONFIG } from '../core/app-config.token';
import { PatientService } from './patient.service';

const mockConfig = { apiUrl: '/api', issuer: '', clientId: '', requireHttps: false };

describe('PatientService', () => {
  let service: PatientService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: mockConfig },
      ],
    });
    service = TestBed.inject(PatientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list() sends GET /api/patients with correct params', () => {
    service.list({ page: 0, size: 20, sort: 'LAST_NAME', direction: 'ASC' }).subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/patients');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('sort')).toBe('LAST_NAME');
    expect(req.request.params.get('direction')).toBe('ASC');
    req.flush({
      _embedded: { patients: [{ id: 'a', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12', _links: {} }] },
      _links: {},
      totalElements: 1, totalPages: 1, page: 0, size: 20,
    });
  });

  it('list() maps HAL response to PatientPage', () => {
    let result: any;
    service.list({ page: 0, size: 20, sort: 'LAST_NAME', direction: 'ASC' }).subscribe(r => result = r);
    httpMock.expectOne(r => r.url === '/api/patients').flush({
      _embedded: { patients: [{ id: 'a', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12', _links: {} }] },
      _links: {},
      totalElements: 1, totalPages: 1, page: 0, size: 20,
    });
    expect(result.patients).toHaveLength(1);
    expect(result.patients[0]).toEqual({ id: 'a', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' });
    expect(result.totalElements).toBe(1);
    expect(result.totalPages).toBe(1);
  });

  it('list() includes search param when provided', () => {
    service.list({ search: 'ali', page: 0, size: 20, sort: 'LAST_NAME', direction: 'ASC' }).subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/patients');
    expect(req.request.params.get('search')).toBe('ali');
    req.flush({ _embedded: { patients: [] }, _links: {}, totalElements: 0, totalPages: 0, page: 0, size: 20 });
  });

  it('list() omits search param when not provided', () => {
    service.list({ page: 0, size: 20, sort: 'LAST_NAME', direction: 'ASC' }).subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/patients');
    expect(req.request.params.has('search')).toBe(false);
    req.flush({ _embedded: { patients: [] }, _links: {}, totalElements: 0, totalPages: 0, page: 0, size: 20 });
  });

  it('get() sends GET /api/patients/:id and maps response', () => {
    let result: any;
    service.get('abc-123').subscribe(r => result = r);
    httpMock.expectOne(r => r.url === '/api/patients/abc-123').flush({
      id: 'abc-123', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12', _links: {},
    });
    expect(result).toEqual({ id: 'abc-123', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' });
  });

  it('register() sends POST /api/patients and maps response', () => {
    let result: any;
    service.register({ firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' }).subscribe(r => result = r);
    const req = httpMock.expectOne(r => r.url === '/api/patients' && r.method === 'POST');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' });
    req.flush({ id: 'new-id', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12', _links: {} });
    expect(result).toEqual({ id: 'new-id', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' });
  });
});
