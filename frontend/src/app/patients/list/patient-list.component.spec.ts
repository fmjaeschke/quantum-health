import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { PatientListComponent } from './patient-list.component';
import { PatientStore } from '../patient.store';
import { AuthStore } from '../../auth/auth.store';

const mockPatients = [
  { id: 'p1', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' },
  { id: 'p2', firstName: 'Bob', lastName: 'Brown', dateOfBirth: '1972-07-04' },
];

function makePatientStore(overrides = {}) {
  return {
    patients: signal(mockPatients),
    totalElements: signal(2),
    totalPages: signal(1),
    page: signal(0),
    pageSize: signal(20),
    search: signal(''),
    sortField: signal('LAST_NAME'),
    sortDirection: signal('ASC'),
    loading: signal(false),
    loadPatients: vi.fn().mockResolvedValue(undefined),
    setSearch: vi.fn().mockResolvedValue(undefined),
    setSort: vi.fn().mockResolvedValue(undefined),
    goToPage: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

function makeAuthStore(roles: string[] = []) {
  return { roles: signal(roles), token: signal(null), login: vi.fn(), logout: vi.fn() };
}

describe('PatientListComponent', () => {
  it('renders patient rows', async () => {
    await render(PatientListComponent, {
      providers: [
        { provide: PatientStore, useValue: makePatientStore() },
        { provide: AuthStore, useValue: makeAuthStore(['DOCTOR']) },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByText('Anderson')).toBeTruthy();
    expect(screen.getByText('Brown')).toBeTruthy();
  });

  it('shows Register Patient button for CLERK role', async () => {
    await render(PatientListComponent, {
      providers: [
        { provide: PatientStore, useValue: makePatientStore() },
        { provide: AuthStore, useValue: makeAuthStore(['CLERK']) },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByText('Register Patient')).toBeTruthy();
  });

  it('hides Register Patient button for DOCTOR role', async () => {
    await render(PatientListComponent, {
      providers: [
        { provide: PatientStore, useValue: makePatientStore() },
        { provide: AuthStore, useValue: makeAuthStore(['DOCTOR']) },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.queryByText('Register Patient')).toBeNull();
  });

  it('shows Register Patient button for ADMIN role', async () => {
    await render(PatientListComponent, {
      providers: [
        { provide: PatientStore, useValue: makePatientStore() },
        { provide: AuthStore, useValue: makeAuthStore(['ADMIN']) },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByText('Register Patient')).toBeTruthy();
  });

  it('calls loadPatients on init', async () => {
    const store = makePatientStore();
    await render(PatientListComponent, {
      providers: [
        { provide: PatientStore, useValue: store },
        { provide: AuthStore, useValue: makeAuthStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(store.loadPatients).toHaveBeenCalled();
  });

  it('navigates to patient detail on row click', async () => {
    const router = { navigate: vi.fn() };
    await render(PatientListComponent, {
      providers: [
        { provide: PatientStore, useValue: makePatientStore() },
        { provide: AuthStore, useValue: makeAuthStore(['DOCTOR']) },
        { provide: Router, useValue: router },
      ],
    });
    await userEvent.click(screen.getByText('Anderson'));
    expect(router.navigate).toHaveBeenCalledWith(['/patients', 'p1']);
  });
});
