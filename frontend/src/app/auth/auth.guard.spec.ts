import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { signal } from '@angular/core';
import { authGuard } from './auth.guard';
import { AuthStore } from './auth.store';

const route = {} as ActivatedRouteSnapshot;
const state = {} as RouterStateSnapshot;

describe('authGuard', () => {
  const mockOAuthService = { hasValidAccessToken: vi.fn() };
  const mockAuthStore = {
    token: signal<string | null>(null),
    roles: signal<string[]>([]),
    login: vi.fn(),
    logout: vi.fn(),
    configure: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: mockOAuthService },
        { provide: AuthStore, useValue: mockAuthStore },
      ],
    });
  });

  it('returns true when OAuthService reports a valid access token', () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(true);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBe(true);
  });

  it('returns false and calls login when no valid token', () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBe(false);
    expect(mockAuthStore.login).toHaveBeenCalled();
  });
});
