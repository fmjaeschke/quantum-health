import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { AuthStore } from './auth.store';

export const authGuard: CanActivateFn = () => {
  if (inject(OAuthService).hasValidAccessToken()) {
    return true;
  }
  inject(AuthStore).login();
  return false;
};
