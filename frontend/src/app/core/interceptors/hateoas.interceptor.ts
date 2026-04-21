import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { HateoasService } from '../hateoas.service';

export const hateoasInterceptor: HttpInterceptorFn = (req, next) => {
  const hateoas = inject(HateoasService);
  return next(req).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        const body = event.body as Record<string, unknown> | null;
        const links = body?.['_links'] as Record<string, { href: string }> | undefined;
        if (links) {
          hateoas.updateLinks(req.url, links);
        }
      }
    })
  );
};
