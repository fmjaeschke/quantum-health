import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { APP_CONFIG } from '../core/app-config.token';
import { Patient, PatientPage, RegisterPatientRequest, SortDirection, SortField } from './patient.model';

interface ListParams {
  search?: string;
  page: number;
  size: number;
  sort: SortField;
  direction: SortDirection;
}

interface PatientHalResponse {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
}

interface PatientPageHalResponse {
  _embedded: { patients: PatientHalResponse[] };
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(APP_CONFIG).apiUrl;

  list(params: ListParams): Observable<PatientPage> {
    let httpParams = new HttpParams()
      .set('page', String(params.page))
      .set('size', String(params.size))
      .set('sort', params.sort)
      .set('direction', params.direction);
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }
    return this.http
      .get<PatientPageHalResponse>(`${this.apiUrl}/patients`, { params: httpParams })
      .pipe(
        map(body => ({
          patients: body._embedded.patients.map(p => ({
            id: p.id,
            firstName: p.firstName,
            lastName: p.lastName,
            dateOfBirth: p.dateOfBirth,
          })),
          totalElements: body.totalElements,
          totalPages: body.totalPages,
          page: body.page,
          size: body.size,
        }))
      );
  }

  get(id: string): Observable<Patient> {
    return this.http
      .get<PatientHalResponse>(`${this.apiUrl}/patients/${id}`)
      .pipe(map(body => ({ id: body.id, firstName: body.firstName, lastName: body.lastName, dateOfBirth: body.dateOfBirth })));
  }

  register(req: RegisterPatientRequest): Observable<Patient> {
    return this.http
      .post<PatientHalResponse>(`${this.apiUrl}/patients`, req)
      .pipe(map(body => ({ id: body.id, firstName: body.firstName, lastName: body.lastName, dateOfBirth: body.dateOfBirth })));
  }
}
