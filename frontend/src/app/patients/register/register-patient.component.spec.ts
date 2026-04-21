import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { RegisterPatientComponent } from './register-patient.component';
import { PatientStore } from '../patient.store';

function makeStore(overrides = {}) {
  return {
    loading: signal(false),
    registerPatient: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

describe('RegisterPatientComponent', () => {
  it('renders first name, last name, and date of birth fields', async () => {
    await render(RegisterPatientComponent, {
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    expect(screen.getByPlaceholderText('First name')).toBeTruthy();
    expect(screen.getByPlaceholderText('Last name')).toBeTruthy();
    expect(screen.getByText('Date of birth')).toBeTruthy();
  });

  it('shows validation error when first name is empty and form submitted', async () => {
    await render(RegisterPatientComponent, {
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    await userEvent.click(screen.getByRole('button', { name: /register/i }));
    expect(screen.getByText('First name is required')).toBeTruthy();
  });

  it('shows validation error when last name is empty and form submitted', async () => {
    await render(RegisterPatientComponent, {
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    await userEvent.click(screen.getByRole('button', { name: /register/i }));
    expect(screen.getByText('Last name is required')).toBeTruthy();
  });

  it('cancel button navigates to /patients', async () => {
    const router = { navigate: vi.fn() };
    await render(RegisterPatientComponent, {
      providers: [
        { provide: PatientStore, useValue: makeStore() },
        { provide: Router, useValue: router },
      ],
    });
    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(router.navigate).toHaveBeenCalledWith(['/patients']);
  });

  it('does not call registerPatient when form is invalid', async () => {
    const store = makeStore();
    await render(RegisterPatientComponent, {
      providers: [
        { provide: PatientStore, useValue: store },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    await userEvent.click(screen.getByRole('button', { name: /register/i }));
    expect(store.registerPatient).not.toHaveBeenCalled();
  });

  it('calls registerPatient with correct data when form is valid', async () => {
    const store = makeStore();
    const { fixture } = await render(RegisterPatientComponent, {
      providers: [
        { provide: PatientStore, useValue: store },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    const comp = fixture.componentInstance;
    // Set values directly on the form (bypasses DatePicker UI which is complex in tests)
    comp.form.setValue({ firstName: 'Alice', lastName: 'Anderson', dateOfBirth: new Date(1985, 2, 12) });
    comp.onSubmit();
    expect(store.registerPatient).toHaveBeenCalledWith({
      firstName: 'Alice',
      lastName: 'Anderson',
      dateOfBirth: '1985-03-12',
    });
  });
});
