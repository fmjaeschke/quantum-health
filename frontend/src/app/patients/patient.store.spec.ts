import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PatientService } from './patient.service';
import { PatientStore } from './patient.store';

const mockPatient = { id: 'p1', firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' };
const mockPage = { patients: [mockPatient], totalElements: 1, totalPages: 1, page: 0, size: 20 };

describe('PatientStore', () => {
  const mockPatientService = {
    list: vi.fn().mockReturnValue(of(mockPage)),
    get: vi.fn().mockReturnValue(of(mockPatient)),
    register: vi.fn().mockReturnValue(of(mockPatient)),
  };
  const mockRouter = { navigate: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    mockPatientService.list.mockReturnValue(of(mockPage));
    mockPatientService.get.mockReturnValue(of(mockPatient));
    mockPatientService.register.mockReturnValue(of(mockPatient));
    TestBed.configureTestingModule({
      providers: [
        { provide: PatientService, useValue: mockPatientService },
        { provide: Router, useValue: mockRouter },
      ],
    });
  });

  it('starts with empty state', () => {
    const store = TestBed.inject(PatientStore);
    expect(store.patients()).toEqual([]);
    expect(store.currentPatient()).toBeNull();
    expect(store.totalElements()).toBe(0);
    expect(store.page()).toBe(0);
    expect(store.loading()).toBe(false);
    expect(store.search()).toBe('');
    expect(store.sortField()).toBe('LAST_NAME');
    expect(store.sortDirection()).toBe('ASC');
  });

  it('loadPatients() calls service with current state params and populates patients', async () => {
    const store = TestBed.inject(PatientStore);
    await store.loadPatients();
    expect(mockPatientService.list).toHaveBeenCalledWith({
      page: 0, size: 20, sort: 'LAST_NAME', direction: 'ASC',
    });
    expect(store.patients()).toEqual([mockPatient]);
    expect(store.totalElements()).toBe(1);
    expect(store.loading()).toBe(false);
  });

  it('setSearch() resets page to 0, updates search, and triggers load', async () => {
    const store = TestBed.inject(PatientStore);
    // simulate being on page 2
    await store.goToPage(2);
    vi.clearAllMocks();
    mockPatientService.list.mockReturnValue(of(mockPage));
    await store.setSearch('alice');
    expect(store.search()).toBe('alice');
    expect(store.page()).toBe(0);
    expect(mockPatientService.list).toHaveBeenCalledWith(
      expect.objectContaining({ search: 'alice', page: 0 })
    );
  });

  it('setSort() with same field toggles direction', async () => {
    const store = TestBed.inject(PatientStore);
    // default is LAST_NAME ASC
    await store.setSort('LAST_NAME');
    expect(store.sortField()).toBe('LAST_NAME');
    expect(store.sortDirection()).toBe('DESC');
  });

  it('setSort() with different field resets direction to ASC', async () => {
    const store = TestBed.inject(PatientStore);
    await store.setSort('FIRST_NAME');
    expect(store.sortField()).toBe('FIRST_NAME');
    expect(store.sortDirection()).toBe('ASC');
  });

  it('setSort() resets page to 0', async () => {
    const store = TestBed.inject(PatientStore);
    await store.goToPage(2);
    await store.setSort('FIRST_NAME');
    expect(store.page()).toBe(0);
  });

  it('goToPage() updates page and triggers load', async () => {
    const store = TestBed.inject(PatientStore);
    await store.goToPage(3);
    expect(store.page()).toBe(3);
    expect(mockPatientService.list).toHaveBeenCalledWith(
      expect.objectContaining({ page: 3 })
    );
  });

  it('loadPatient() sets currentPatient', async () => {
    const store = TestBed.inject(PatientStore);
    await store.loadPatient('p1');
    expect(mockPatientService.get).toHaveBeenCalledWith('p1');
    expect(store.currentPatient()).toEqual(mockPatient);
    expect(store.loading()).toBe(false);
  });

  it('registerPatient() calls service and navigates to patient detail', async () => {
    const store = TestBed.inject(PatientStore);
    await store.registerPatient({ firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12' });
    expect(mockPatientService.register).toHaveBeenCalledWith({
      firstName: 'Alice', lastName: 'Anderson', dateOfBirth: '1985-03-12',
    });
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients', 'p1']);
  });

  it('loadPatients() sets loading false even on error', async () => {
    mockPatientService.list.mockReturnValue(throwError(() => new Error('network error')));
    const store = TestBed.inject(PatientStore);
    await store.loadPatients();
    expect(store.loading()).toBe(false);
  });

  it('loadPatients() passes search term when set', async () => {
    const store = TestBed.inject(PatientStore);
    await store.setSearch('anderson');
    expect(mockPatientService.list).toHaveBeenCalledWith(
      expect.objectContaining({ search: 'anderson' })
    );
  });
});
