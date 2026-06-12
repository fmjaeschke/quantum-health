export type PrescriptionStatus = 'ISSUED' | 'FULFILLED' | 'CANCELLED' | 'EXPIRED';

export interface HalLink {
  href: string;
}

export interface MedicationItem {
  drugName: string;
  dosage: string;
  frequency: string;
}

export interface PrescriptionResource {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  medications: MedicationItem[];
  status: PrescriptionStatus;
  issuedAt: string;
  fulfilledAt?: string;
  fulfilledBy?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancelledReason?: string;
  expiredAt?: string;
  _links?: {
    self: HalLink;
    'fulfill-prescription'?: HalLink;
    'cancel-prescription'?: HalLink;
  };
}

export interface IssuePrescriptionRequest {
  patientId: string;
  medications: MedicationItem[];
}

export interface PrescriptionListResponse {
  _embedded: { prescriptions: PrescriptionResource[] };
  _links: { self: { href: string } };
  page: number;
  pageSize: number;
  totalElements: number;
}
