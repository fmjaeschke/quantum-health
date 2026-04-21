import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../../auth/auth.store';
import { GlobalErrorService } from '../global-error.service';
import { APP_CONFIG } from '../app-config.token';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const errors = inject(GlobalErrorService);
  const apiUrl = inject(APP_CONFIG).apiUrl;

  if (!req.url.startsWith(apiUrl)) return next(req);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        auth.logout();
      } else if (err.status === 403) {
        errors.show('Access denied', (err.error as { detail?: string })?.detail ?? err.message);
      } else {
        errors.show('Error', (err.error as { detail?: string })?.detail ?? err.message);
      }
      return throwError(() => err);
    })
  );
};
