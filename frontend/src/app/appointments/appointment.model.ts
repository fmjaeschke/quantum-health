export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED';

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
  status: AppointmentStatus;
  _links?: {
    self: HalLink;
    confirm?: HalLink;
    cancel?: HalLink;
  };
}

export interface ScheduleAppointmentRequest {
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  scheduledAt: string;
}
