import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { roleGuard } from './role.guard';
import { AuthStore } from './auth.store';

const route = {} as ActivatedRouteSnapshot;
const state = {} as RouterStateSnapshot;

describe('roleGuard', () => {
  const mockAuthStore = {
    token: signal<string | null>('token'),
    roles: signal<string[]>(['DOCTOR', 'ADMIN']),
    login: vi.fn(),
    logout: vi.fn(),
    configure: vi.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthStore, useValue: mockAuthStore }],
    });
  });

  it('returns true when user has the required role', () => {
    const guard = roleGuard('DOCTOR');
    const result = TestBed.runInInjectionContext(() => guard(route, state));
    expect(result).toBe(true);
  });

  it('returns false when user does not have the required role', () => {
    const guard = roleGuard('PHARMACIST');
    const result = TestBed.runInInjectionContext(() => guard(route, state));
    expect(result).toBe(false);
  });

  it('returns false when user has no roles', () => {
    const noRoleStore = { ...mockAuthStore, roles: signal<string[]>([]) };
    TestBed.overrideProvider(AuthStore, { useValue: noRoleStore });
    const guard = roleGuard('ADMIN');
    const result = TestBed.runInInjectionContext(() => guard(route, state));
    expect(result).toBe(false);
  });
});
