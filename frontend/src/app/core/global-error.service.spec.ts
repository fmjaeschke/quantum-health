import { TestBed } from '@angular/core/testing';
import { GlobalErrorService } from './global-error.service';

describe('GlobalErrorService', () => {
  let service: GlobalErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GlobalErrorService);
  });

  it('starts with null lastError', () => {
    expect(service.lastError()).toBeNull();
  });

  it('show() sets lastError with summary and detail', () => {
    service.show('Access denied', 'You cannot view this resource');
    expect(service.lastError()).toEqual({
      summary: 'Access denied',
      detail: 'You cannot view this resource',
    });
  });

  it('clear() resets lastError to null', () => {
    service.show('Error', 'Something went wrong');
    service.clear();
    expect(service.lastError()).toBeNull();
  });

  it('show() replaces a previous error', () => {
    service.show('First', 'first detail');
    service.show('Second', 'second detail');
    expect(service.lastError()?.summary).toBe('Second');
  });
});
