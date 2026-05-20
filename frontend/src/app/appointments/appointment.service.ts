import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { APP_CONFIG } from '../core/app-config.token';
import { AppointmentListPage, AppointmentQuery, AppointmentResource, ScheduleAppointmentRequest } from './appointment.model';

interface AppointmentListHalResponse {
  _embedded?: { appointments: AppointmentResource[] };
  page: number;
  pageSize: number;
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(APP_CONFIG).apiUrl;

  schedule(req: ScheduleAppointmentRequest): Observable<AppointmentResource> {
    return this.http.post<AppointmentResource>(`${this.apiUrl}/appointments`, req);
  }

  get(id: string): Observable<AppointmentResource> {
    return this.http.get<AppointmentResource>(`${this.apiUrl}/appointments/${id}`);
  }

  list(query: AppointmentQuery): Observable<AppointmentListPage> {
    let params = new HttpParams();
    if (query.status != null) params = params.set('status', query.status);
    if (query.page != null) params = params.set('page', String(query.page));
    if (query.pageSize != null) params = params.set('size', String(query.pageSize));

    return this.http
      .get<AppointmentListHalResponse>(`${this.apiUrl}/appointments`, { params })
      .pipe(map(body => ({
        appointments: body._embedded?.appointments ?? [],
        page: body.page,
        pageSize: body.pageSize,
        totalElements: body.totalElements,
      })));
  }

  postTransition(href: string): Observable<AppointmentResource> {
    return this.http.post<AppointmentResource>(href, {});
  }
}
