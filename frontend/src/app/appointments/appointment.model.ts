export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'ARRIVED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface HalLink {
  href: string;
}

export interface AppointmentResource {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  scheduledAt: string;
  reason: string;
  status: AppointmentStatus;
  _links?: {
    self: HalLink;
    confirm?: HalLink;
    cancel?: HalLink;
    'check-in'?: HalLink;
    start?: HalLink;
  };
}

export interface ScheduleAppointmentRequest {
  patientId: string;
  doctorId: string;
  scheduledAt: string;
  reason: string;
}

export interface AppointmentQuery {
  status?: AppointmentStatus;
  page?: number;
  pageSize?: number;
}

export interface AppointmentListPage {
  appointments: AppointmentResource[];
  page: number;
  pageSize: number;
  totalElements: number;
}

export interface Doctor {
  id: string;
  displayName: string;
}
