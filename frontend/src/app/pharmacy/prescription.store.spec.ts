import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PrescriptionService } from './prescription.service';
import { PrescriptionStore } from './prescription.store';

const RX_ID = 'rx-1';

const mockIssued = {
  id: RX_ID,
  patientId: 'p-1',
  patientName: 'Alice Smith',
  doctorId: 'dr-smith',
  doctorName: 'Dr. Smith',
  medications: [{ drugName: 'Aspirin', dosage: '100mg', frequency: 'once daily' }],
  status: 'ISSUED' as const,
  issuedAt: '2026-01-15T10:00:00Z',
  _links: {
    self: { href: '/api/prescriptions/rx-1' },
    'fulfill-prescription': { href: '/api/prescriptions/rx-1/fulfill' },
  },
};

const mockListResponse = {
  _embedded: { prescriptions: [mockIssued] },
  _links: { self: { href: '/api/prescriptions' } },
  page: 0,
  pageSize: 20,
  totalElements: 1,
};

describe('PrescriptionStore', () => {
  const mockCancelled = { ...mockIssued, status: 'CANCELLED' as const };

  const mockService = {
    get: vi.fn().mockReturnValue(of(mockIssued)),
    issue: vi.fn(),
    list: vi.fn().mockReturnValue(of(mockListResponse)),
    postAction: vi.fn().mockReturnValue(of({ ...mockIssued, status: 'FULFILLED' })),
    postActionWithBody: vi.fn().mockReturnValue(of(mockCancelled)),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockService.get.mockReturnValue(of(mockIssued));
    mockService.list.mockReturnValue(of(mockListResponse));
    mockService.postAction.mockReturnValue(of({ ...mockIssued, status: 'FULFILLED' }));
    mockService.postActionWithBody.mockReturnValue(of(mockCancelled));

    TestBed.configureTestingModule({
      providers: [
        { provide: PrescriptionService, useValue: mockService },
      ],
    });
  });

  it('starts with empty state', () => {
    const store = TestBed.inject(PrescriptionStore);
    expect(store.prescriptions()).toEqual([]);
    expect(store.currentPrescription()).toBeNull();
    expect(store.loading()).toBe(false);
  });

  it('loadPrescription() fetches by id and stores it', async () => {
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescription(RX_ID);
    expect(mockService.get).toHaveBeenCalledWith(RX_ID);
    expect(store.currentPrescription()?.id).toBe(RX_ID);
    expect(store.currentPrescription()?.patientName).toBe('Alice Smith');
    expect(store.loading()).toBe(false);
  });

  it('loadPrescription() clears currentPrescription before fetching', async () => {
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescription(RX_ID);
    mockService.get.mockReturnValue(throwError(() => new Error('fail')));
    await store.loadPrescription('other-id');
    expect(store.currentPrescription()).toBeNull();
  });

  it('loadPrescription() sets loading false on error', async () => {
    mockService.get.mockReturnValue(throwError(() => new Error('network')));
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescription(RX_ID);
    expect(store.loading()).toBe(false);
  });

  it('fulfill() POSTs to fulfill-prescription HAL link', async () => {
    const store = TestBed.inject(PrescriptionStore);
    await store.fulfill(mockIssued);
    expect(mockService.postAction)
      .toHaveBeenCalledWith('/api/prescriptions/rx-1/fulfill');
    expect(store.loading()).toBe(false);
  });

  it('fulfill() does nothing when fulfill-prescription link is absent', async () => {
    const store = TestBed.inject(PrescriptionStore);
    const noLink = { ...mockIssued, _links: { self: { href: '/api/prescriptions/rx-1' } } };
    await store.fulfill(noLink as any);
    expect(mockService.postAction).not.toHaveBeenCalled();
  });

  it('fulfill() updates currentPrescription from the response body', async () => {
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescription(RX_ID);
    await store.fulfill(mockIssued);
    expect(store.currentPrescription()?.status).toBe('FULFILLED');
  });

  it('fulfill() updates the matching entry in prescriptions', async () => {
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescriptions();
    await store.fulfill(mockIssued);
    expect(store.prescriptions()[0].status).toBe('FULFILLED');
  });

  it('loadPrescriptions() fetches list and populates prescriptions signal', async () => {
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescriptions();
    expect(mockService.list).toHaveBeenCalledWith(0, 20);
    expect(store.prescriptions()).toHaveLength(1);
    expect(store.prescriptions()[0].patientName).toBe('Alice Smith');
    expect(store.loading()).toBe(false);
  });

  it('loadPrescriptions() sets loading false on error', async () => {
    mockService.list.mockReturnValue(throwError(() => new Error('network')));
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescriptions();
    expect(store.loading()).toBe(false);
    expect(store.prescriptions()).toEqual([]);
  });

  it('cancel() POSTs to cancel-prescription HAL link with reason and returns true', async () => {
    const rxWithCancelLink = {
      ...mockIssued,
      _links: {
        self: { href: '/api/prescriptions/rx-1' },
        'cancel-prescription': { href: '/api/prescriptions/rx-1/cancel' },
      },
    };
    const store = TestBed.inject(PrescriptionStore);
    const result = await store.cancel(rxWithCancelLink, 'Prescribing error');
    expect(mockService.postActionWithBody)
      .toHaveBeenCalledWith('/api/prescriptions/rx-1/cancel', { reason: 'Prescribing error' });
    expect(store.loading()).toBe(false);
    expect(result).toBe(true);
  });

  it('cancel() does nothing and returns false when cancel-prescription link is absent', async () => {
    const store = TestBed.inject(PrescriptionStore);
    const result = await store.cancel(mockIssued as any, 'reason');
    expect(mockService.postActionWithBody).not.toHaveBeenCalled();
    expect(result).toBe(false);
  });

  it('cancel() returns false and sets loading false on error', async () => {
    const rxWithCancelLink = {
      ...mockIssued,
      _links: {
        self: { href: '/api/prescriptions/rx-1' },
        'cancel-prescription': { href: '/api/prescriptions/rx-1/cancel' },
      },
    };
    mockService.postActionWithBody.mockReturnValue(throwError(() => new Error('conflict')));
    const store = TestBed.inject(PrescriptionStore);
    const result = await store.cancel(rxWithCancelLink, 'Prescribing error');
    expect(store.loading()).toBe(false);
    expect(result).toBe(false);
  });

  it('cancel() updates currentPrescription from the response body', async () => {
    const rxWithCancelLink = {
      ...mockIssued,
      _links: {
        self: { href: '/api/prescriptions/rx-1' },
        'cancel-prescription': { href: '/api/prescriptions/rx-1/cancel' },
      },
    };
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescription(RX_ID);
    await store.cancel(rxWithCancelLink, 'Prescribing error');
    expect(store.currentPrescription()?.status).toBe('CANCELLED');
  });

  it('cancel() updates the matching entry in prescriptions', async () => {
    const rxWithCancelLink = {
      ...mockIssued,
      _links: {
        self: { href: '/api/prescriptions/rx-1' },
        'cancel-prescription': { href: '/api/prescriptions/rx-1/cancel' },
      },
    };
    const store = TestBed.inject(PrescriptionStore);
    await store.loadPrescriptions();
    await store.cancel(rxWithCancelLink, 'Prescribing error');
    expect(store.prescriptions()[0].status).toBe('CANCELLED');
  });
});
