import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { hateoasInterceptor } from './hateoas.interceptor';
import { HateoasService } from '../hateoas.service';

describe('hateoasInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let hateoasService: HateoasService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([hateoasInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    hateoasService = TestBed.inject(HateoasService);
  });

  afterEach(() => controller.verify());

  it('calls updateLinks when response body contains _links', () => {
    const updateSpy = vi.spyOn(hateoasService, 'updateLinks');
    http.get('/api/patients/1').subscribe();
    controller.expectOne('/api/patients/1').flush({
      id: '1',
      _links: { self: { href: '/api/patients/1' }, edit: { href: '/api/patients/1' } },
    });
    expect(updateSpy).toHaveBeenCalledWith('/api/patients/1', {
      self: { href: '/api/patients/1' },
      edit: { href: '/api/patients/1' },
    });
  });

  it('does not call updateLinks when response has no _links', () => {
    const updateSpy = vi.spyOn(hateoasService, 'updateLinks');
    http.get('/api/patients/1').subscribe();
    controller.expectOne('/api/patients/1').flush({ id: '1', name: 'John' });
    expect(updateSpy).not.toHaveBeenCalled();
  });

  it('does not call updateLinks on error responses', () => {
    const updateSpy = vi.spyOn(hateoasService, 'updateLinks');
    http.get('/api/patients/1').subscribe({ error: () => {} });
    controller.expectOne('/api/patients/1').flush(
      { detail: 'Not found' },
      { status: 404, statusText: 'Not Found' }
    );
    expect(updateSpy).not.toHaveBeenCalled();
  });

  it('does not call updateLinks when _links is null', () => {
    const updateSpy = vi.spyOn(hateoasService, 'updateLinks');
    http.get('/api/patients/1').subscribe();
    controller.expectOne('/api/patients/1').flush({ id: '1', _links: null });
    expect(updateSpy).not.toHaveBeenCalled();
  });

  it('calls updateLinks with top-level _links from a collection response', () => {
    const updateSpy = vi.spyOn(hateoasService, 'updateLinks');
    http.get('/api/patients').subscribe();
    controller.expectOne('/api/patients').flush({
      _embedded: { patients: [{ id: '1' }] },
      _links: { self: { href: '/api/patients' }, next: { href: '/api/patients?page=1' } },
    });
    expect(updateSpy).toHaveBeenCalledWith('/api/patients', {
      self: { href: '/api/patients' },
      next: { href: '/api/patients?page=1' },
    });
  });
});
