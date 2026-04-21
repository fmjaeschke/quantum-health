import { ApplicationConfig, inject, provideAppInitializer, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { appRoutes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { hateoasInterceptor } from './core/interceptors/hateoas.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { APP_CONFIG, AppConfig } from './core/app-config.token';
import { AuthStore } from './auth/auth.store';

export function appConfig(cfg: AppConfig): ApplicationConfig {
  return {
    providers: [
      provideZonelessChangeDetection(),
      provideRouter(appRoutes, withComponentInputBinding()),
      provideHttpClient(withInterceptors([authInterceptor, hateoasInterceptor, errorInterceptor])),
      provideOAuthClient(),
      providePrimeNG({ theme: { preset: Aura } }),
      { provide: APP_CONFIG, useValue: cfg },
      provideAppInitializer(() => {
        const authStore = inject(AuthStore);
        return authStore.configure();
      }),
    ],
  };
}
