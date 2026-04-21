import { render, screen } from '@testing-library/angular';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { PatientDetailComponent } from './patient-detail.component';
import { PatientStore } from '../patient.store';

const mockPatient = { id: 'p1', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' };

function makeStore(overrides = {}) {
  return {
    currentPatient: signal(mockPatient),
    loading: signal(false),
    loadPatient: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

describe('PatientDetailComponent', () => {
  it('calls loadPatient with the route id on init', async () => {
    const store = makeStore();
    await render(PatientDetailComponent, {
      componentInputs: { id: 'p1' },
      providers: [
        { provide: PatientStore, useValue: store },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(store.loadPatient).toHaveBeenCalledWith('p1');
  });

  it('renders patient full name', async () => {
    await render(PatientDetailComponent, {
      componentInputs: { id: 'p1' },
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByText('Alice Anderson')).toBeTruthy();
  });

  it('renders initials avatar', async () => {
    await render(PatientDetailComponent, {
      componentInputs: { id: 'p1' },
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByText('AA')).toBeTruthy();
  });

  it('renders placeholder sections for appointments and encounters', async () => {
    await render(PatientDetailComponent, {
      componentInputs: { id: 'p1' },
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByText(/Appointments/i)).toBeTruthy();
    expect(screen.getByText(/Encounters/i)).toBeTruthy();
  });

  it('shows nothing when currentPatient is null (loading)', async () => {
    const store = makeStore({ currentPatient: signal(null), loading: signal(true) });
    await render(PatientDetailComponent, {
      componentInputs: { id: 'p1' },
      providers: [
        { provide: PatientStore, useValue: store },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.queryByText('Alice Anderson')).toBeNull();
  });
});
