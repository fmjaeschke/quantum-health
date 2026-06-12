import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../core/app-config.token';
import { IssuePrescriptionRequest, PrescriptionListResponse, PrescriptionResource } from './prescription.model';

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(APP_CONFIG).apiUrl;

  list(page = 0, size = 20): Observable<PrescriptionListResponse> {
    return this.http.get<PrescriptionListResponse>(
      `${this.apiUrl}/prescriptions?page=${page}&size=${size}`);
  }

  get(id: string): Observable<PrescriptionResource> {
    return this.http.get<PrescriptionResource>(`${this.apiUrl}/prescriptions/${id}`);
  }

  issue(req: IssuePrescriptionRequest): Observable<PrescriptionResource> {
    return this.http.post<PrescriptionResource>(`${this.apiUrl}/prescriptions`, req);
  }

  postAction(href: string): Observable<PrescriptionResource> {
    return this.http.post<PrescriptionResource>(href, {});
  }

  postActionWithBody(href: string, body: unknown): Observable<PrescriptionResource> {
    return this.http.post<PrescriptionResource>(href, body);
  }
}
