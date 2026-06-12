import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { signal } from '@angular/core';
import { PharmacistDashboardComponent } from './pharmacist-dashboard.component';
import { PrescriptionStore } from '../prescription.store';

const mockRx = {
  id: 'rx-1',
  patientId: 'p-1',
  patientName: 'Alice Smith',
  doctorId: 'dr-1',
  doctorName: 'Dr. Smith',
  medications: [{ drugName: 'Aspirin', dosage: '100mg', frequency: 'once daily' }],
  status: 'ISSUED' as const,
  issuedAt: '2026-01-15T10:00:00Z',
  _links: {
    self: { href: '/api/prescriptions/rx-1' },
    'cancel-prescription': { href: '/api/prescriptions/rx-1/cancel' },
  },
};

function makeStore(overrides: Record<string, unknown> = {}) {
  return {
    loading: signal(false),
    prescriptions: signal([mockRx]),
    cancel: vi.fn().mockResolvedValue(true),
    fulfill: vi.fn().mockResolvedValue(undefined),
    loadPrescriptions: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

const providers = (store: ReturnType<typeof makeStore>) => [
  { provide: PrescriptionStore, useValue: store },
];

describe('PharmacistDashboardComponent', () => {
  it('opens cancel dialog when Cancel row button is clicked', async () => {
    const store = makeStore();
    await render(PharmacistDashboardComponent, { providers: providers(store) });

    expect(screen.queryByRole('dialog')).toBeNull();

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByRole('dialog')).toBeTruthy();
  });

  it('calls store.cancel with entered reason and closes dialog on Confirm', async () => {
    const store = makeStore();
    await render(PharmacistDashboardComponent, { providers: providers(store) });

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    await userEvent.type(screen.getByPlaceholderText('Enter reason'), 'Wrong dosage');
    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    expect(store.cancel).toHaveBeenCalledWith(mockRx, 'Wrong dosage');
    expect(store.loadPrescriptions).toHaveBeenCalledTimes(1); // only on init
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('does not call store.cancel when reason is empty', async () => {
    const store = makeStore();
    await render(PharmacistDashboardComponent, { providers: providers(store) });

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    expect(store.cancel).not.toHaveBeenCalled();
  });

  it('closes dialog without calling store.cancel when Close is clicked', async () => {
    const store = makeStore();
    await render(PharmacistDashboardComponent, { providers: providers(store) });

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByRole('dialog')).toBeTruthy();

    await userEvent.click(screen.getByRole('button', { name: 'Close' }));

    expect(store.cancel).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('keeps dialog open with reason preserved when store.cancel fails', async () => {
    const store = makeStore({ cancel: vi.fn().mockResolvedValue(false) });
    await render(PharmacistDashboardComponent, { providers: providers(store) });

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    await userEvent.type(screen.getByPlaceholderText('Enter reason'), 'Wrong dosage');
    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    expect(store.cancel).toHaveBeenCalledWith(mockRx, 'Wrong dosage');
    expect(screen.getByRole('dialog')).toBeTruthy();
    expect((screen.getByPlaceholderText('Enter reason') as HTMLInputElement).value).toBe('Wrong dosage');
  });
});
