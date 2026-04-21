import { TestBed } from '@angular/core/testing';
import { HateoasService } from './hateoas.service';

describe('HateoasService', () => {
  let service: HateoasService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(HateoasService);
  });

  it('can() returns false when no links stored for URL', () => {
    expect(service.can('/api/patients/1', 'edit')()).toBe(false);
  });

  it('can() returns true after updateLinks with matching rel', () => {
    service.updateLinks('/api/patients/1', { edit: { href: '/api/patients/1' } });
    expect(service.can('/api/patients/1', 'edit')()).toBe(true);
  });

  it('can() returns false for a rel not in the links', () => {
    service.updateLinks('/api/patients/1', { self: { href: '/api/patients/1' } });
    expect(service.can('/api/patients/1', 'delete')()).toBe(false);
  });

  it('href() returns the href after updateLinks', () => {
    service.updateLinks('/api/patients/1', { self: { href: '/api/patients/1' } });
    expect(service.href('/api/patients/1', 'self')()).toBe('/api/patients/1');
  });

  it('href() returns undefined for unknown rel', () => {
    expect(service.href('/api/patients/1', 'unknown')()).toBeUndefined();
  });

  it('updateLinks replaces existing links for the same URL', () => {
    service.updateLinks('/api/patients/1', { edit: { href: '/edit' } });
    service.updateLinks('/api/patients/1', { delete: { href: '/delete' } });
    expect(service.can('/api/patients/1', 'edit')()).toBe(false);
    expect(service.can('/api/patients/1', 'delete')()).toBe(true);
  });

  it('links for different URLs do not bleed into each other', () => {
    service.updateLinks('/api/patients/1', { edit: { href: '/edit-1' } });
    expect(service.can('/api/patients/2', 'edit')()).toBe(false);
  });

  it('can() signal updates reactively when updateLinks is called after the signal is created', () => {
    const canEdit = service.can('/api/patients/1', 'edit');
    expect(canEdit()).toBe(false);
    service.updateLinks('/api/patients/1', { edit: { href: '/edit' } });
    expect(canEdit()).toBe(true);
  });

  it('href() signal updates reactively when updateLinks is called after the signal is created', () => {
    const editHref = service.href('/api/patients/1', 'edit');
    expect(editHref()).toBeUndefined();
    service.updateLinks('/api/patients/1', { edit: { href: '/edit' } });
    expect(editHref()).toBe('/edit');
  });

  it('updateLinks stores all rels from a multi-rel response', () => {
    service.updateLinks('/api/patients/1', {
      self: { href: '/api/patients/1' },
      edit: { href: '/api/patients/1/edit' },
      delete: { href: '/api/patients/1' },
    });
    expect(service.can('/api/patients/1', 'self')()).toBe(true);
    expect(service.can('/api/patients/1', 'edit')()).toBe(true);
    expect(service.can('/api/patients/1', 'delete')()).toBe(true);
    expect(service.href('/api/patients/1', 'edit')()).toBe('/api/patients/1/edit');
  });

  it('updating one URL does not remove links for another URL', () => {
    service.updateLinks('/api/patients/1', { edit: { href: '/edit-1' } });
    service.updateLinks('/api/patients/2', { edit: { href: '/edit-2' } });
    expect(service.can('/api/patients/1', 'edit')()).toBe(true);
    expect(service.can('/api/patients/2', 'edit')()).toBe(true);
  });

  it('updateLinks with empty object clears all rels for that URL', () => {
    service.updateLinks('/api/patients/1', { edit: { href: '/edit' } });
    service.updateLinks('/api/patients/1', {});
    expect(service.can('/api/patients/1', 'edit')()).toBe(false);
    expect(service.href('/api/patients/1', 'edit')()).toBeUndefined();
  });
});
