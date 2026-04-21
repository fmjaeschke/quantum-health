import { inject } from '@angular/core';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { APP_CONFIG } from '../core/app-config.token';

interface AuthState {
  token: string | null;
  roles: string[];
}

export const AuthStore = signalStore(
  { providedIn: 'root' },
  withState<AuthState>({ token: null, roles: [] }),
  withMethods((store, oauthService = inject(OAuthService), cfg = inject(APP_CONFIG)) => ({
    async configure(): Promise<void> {
      const authConfig: AuthConfig = {
        issuer: cfg.issuer,
        clientId: cfg.clientId,
        requireHttps: cfg.requireHttps,
        redirectUri: globalThis.location.origin,
        responseType: 'code',
        scope: 'openid profile email roles',
      };
      oauthService.configure(authConfig);
      try {
        await oauthService.loadDiscoveryDocumentAndLogin();
      } catch {
        return;
      }
      if (oauthService.hasValidAccessToken()) {
        const token = oauthService.getAccessToken();
        const claims = oauthService.getIdentityClaims() as { realm_access?: { roles?: string[] } } | null;
        const roles = claims?.realm_access?.roles ?? [];
        patchState(store, { token, roles });
      }
    },
    login(): void {
      oauthService.initCodeFlow();
    },
    logout(): void {
      patchState(store, { token: null, roles: [] });
      oauthService.revokeTokenAndLogout();
    },
  }))
);
