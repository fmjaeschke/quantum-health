import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { App } from './app';
import { APP_CONFIG } from './core/app-config.token';

const mockOAuthService = {
  configure: vi.fn(),
  loadDiscoveryDocumentAndLogin: vi.fn().mockResolvedValue(undefined),
  hasValidAccessToken: vi.fn().mockReturnValue(false),
  getAccessToken: vi.fn().mockReturnValue(null),
  getIdentityClaims: vi.fn().mockReturnValue(null),
  revokeTokenAndLogout: vi.fn(),
  initCodeFlow: vi.fn(),
};

const mockConfig = {
  apiUrl: '/api',
  issuer: 'http://localhost:8180/realms/quantum-health',
  clientId: 'quantum-health-frontend',
  requireHttps: false,
};

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: OAuthService, useValue: mockOAuthService },
        { provide: APP_CONFIG, useValue: mockConfig },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
