import { TestBed } from '@angular/core/testing';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { APP_CONFIG } from '../core/app-config.token';
import { AuthStore } from './auth.store';

describe('AuthStore', () => {
  const mockOAuthService = {
    configure: vi.fn(),
    loadDiscoveryDocumentAndLogin: vi.fn().mockResolvedValue(undefined),
    hasValidAccessToken: vi.fn().mockReturnValue(false),
    getAccessToken: vi.fn().mockReturnValue('mock-access-token'),
    getIdentityClaims: vi.fn().mockReturnValue({ realm_access: { roles: ['DOCTOR'] } }),
    revokeTokenAndLogout: vi.fn(),
    initCodeFlow: vi.fn(),
  };

  const mockConfig = {
    apiUrl: '/api',
    issuer: 'http://localhost:8180/realms/quantum-health',
    clientId: 'quantum-health-frontend',
    requireHttps: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: mockOAuthService },
        { provide: APP_CONFIG, useValue: mockConfig },
      ],
    });
  });

  it('starts with null token and empty roles', () => {
    const store = TestBed.inject(AuthStore);
    expect(store.token()).toBeNull();
    expect(store.roles()).toEqual([]);
  });

  it('configure() calls oauthService.configure with correct OIDC config', async () => {
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(mockOAuthService.configure).toHaveBeenCalledWith(
      expect.objectContaining<Partial<AuthConfig>>({
        issuer: 'http://localhost:8180/realms/quantum-health',
        clientId: 'quantum-health-frontend',
        requireHttps: false,
        redirectUri: window.location.origin,
        responseType: 'code',
        scope: 'openid profile email roles',
      })
    );
  });

  it('configure() calls loadDiscoveryDocumentAndLogin', async () => {
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(mockOAuthService.loadDiscoveryDocumentAndLogin).toHaveBeenCalled();
  });

  it('configure() sets token and roles when valid access token exists after login', async () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(true);
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(store.token()).toBe('mock-access-token');
    expect(store.roles()).toEqual(['DOCTOR']);
  });

  it('configure() does not throw when loadDiscoveryDocumentAndLogin rejects', async () => {
    mockOAuthService.loadDiscoveryDocumentAndLogin.mockRejectedValueOnce(new Error('jwks load error'));
    const store = TestBed.inject(AuthStore);
    await expect(store.configure()).resolves.toBeUndefined();
  });

  it('configure() leaves token null when no valid access token', async () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(false);
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(store.token()).toBeNull();
  });

  it('logout() clears token and roles', () => {
    const store = TestBed.inject(AuthStore);
    store.logout();
    expect(store.token()).toBeNull();
    expect(store.roles()).toEqual([]);
  });

  it('logout() calls revokeTokenAndLogout on OAuthService', () => {
    const store = TestBed.inject(AuthStore);
    store.logout();
    expect(mockOAuthService.revokeTokenAndLogout).toHaveBeenCalled();
  });

  it('login() calls initCodeFlow', () => {
    const store = TestBed.inject(AuthStore);
    store.login();
    expect(mockOAuthService.initCodeFlow).toHaveBeenCalled();
  });

  it('configure() sets roles to [] when getIdentityClaims returns null', async () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(true);
    mockOAuthService.getIdentityClaims.mockReturnValue(null);
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(store.roles()).toEqual([]);
  });

  it('configure() sets roles to [] when claims have no realm_access', async () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(true);
    mockOAuthService.getIdentityClaims.mockReturnValue({ sub: 'user-1' });
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(store.roles()).toEqual([]);
  });

  it('configure() sets roles to [] when realm_access has no roles field', async () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(true);
    mockOAuthService.getIdentityClaims.mockReturnValue({ realm_access: {} });
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(store.roles()).toEqual([]);
  });

  it('logout() clears token and roles that were previously set', async () => {
    mockOAuthService.hasValidAccessToken.mockReturnValue(true);
    const store = TestBed.inject(AuthStore);
    await store.configure();
    expect(store.token()).toBe('mock-access-token');
    store.logout();
    expect(store.token()).toBeNull();
    expect(store.roles()).toEqual([]);
  });
});
