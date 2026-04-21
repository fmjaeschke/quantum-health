import { InjectionToken } from '@angular/core';

export interface AppConfig {
  apiUrl: string;
  issuer: string;
  clientId: string;
  requireHttps: boolean;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
