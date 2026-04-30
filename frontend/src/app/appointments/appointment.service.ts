import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { APP_CONFIG } from '../core/app-config.token';
import { AppointmentResource, ScheduleAppointmentRequest } from './appointment.model';

interface AppointmentListHalResponse {
  _embedded?: { appointments: AppointmentResource[] };
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

  list(): Observable<AppointmentResource[]> {
    return this.http
      .get<AppointmentListHalResponse>(`${this.apiUrl}/appointments`)
      .pipe(map(body => body._embedded?.appointments ?? []));
  }

  postTransition(href: string): Observable<AppointmentResource> {
    return this.http.post<AppointmentResource>(href, {});
  }
}
