export interface Patient {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string; // ISO date string, e.g. '1985-03-12'
}

export interface PatientPage {
  patients: Patient[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface RegisterPatientRequest {
  firstName: string;
  lastName: string;
  dateOfBirth: string; // ISO date string, e.g. '1985-03-12'
}

export type SortField = 'FIRST_NAME' | 'LAST_NAME' | 'DATE_OF_BIRTH';
export type SortDirection = 'ASC' | 'DESC';
