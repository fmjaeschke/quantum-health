import { Component, computed, DestroyRef, inject, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { PatientStore } from '../patient.store';
import { AuthStore } from '../../auth/auth.store';
import { Patient } from '../patient.model';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, InputTextModule, PaginatorModule, SkeletonModule, ButtonModule],
  template: `
    <div class="patient-list">
      <div class="patient-list__header">
        <h2>Patients</h2>
        @if (canRegister()) {
          <button pButton label="Register Patient" icon="pi pi-plus"
                  (click)="router.navigate(['/patients', 'new'])"></button>
        }
      </div>

      <div class="patient-list__search">
        <input pInputText [formControl]="searchControl" placeholder="Search patients…" class="w-full" />
      </div>

      <table class="patient-list__table">
        <thead>
          <tr>
            <th (click)="store.setSort('LAST_NAME')">
              Last Name {{ sortIndicator('LAST_NAME') }}
            </th>
            <th (click)="store.setSort('FIRST_NAME')">
              First Name {{ sortIndicator('FIRST_NAME') }}
            </th>
            <th (click)="store.setSort('DATE_OF_BIRTH')">
              Date of Birth {{ sortIndicator('DATE_OF_BIRTH') }}
            </th>
          </tr>
        </thead>
        <tbody>
          @if (store.loading()) {
            @for (_ of [1, 2, 3]; track $index) {
              <tr><td colspan="3"><p-skeleton height="1.5rem" /></td></tr>
            }
          } @else {
            @for (patient of store.patients(); track patient.id) {
              <tr (click)="navigateToDetail(patient)" class="patient-list__row">
                <td>{{ patient.lastName }}</td>
                <td>{{ patient.firstName }}</td>
                <td>{{ patient.dateOfBirth | date:'d MMM y' }}</td>
              </tr>
            }
            @if (store.patients().length === 0) {
              <tr><td colspan="3" class="patient-list__empty">No patients found.</td></tr>
            }
          }
        </tbody>
      </table>

      <div class="patient-list__footer">
        <span>{{ store.totalElements() }} patient{{ store.totalElements() === 1 ? '' : 's' }}</span>
        <p-paginator
          [rows]="store.pageSize()"
          [totalRecords]="store.totalElements()"
          [first]="store.page() * store.pageSize()"
          (onPageChange)="onPageChange($event)"
        />
      </div>
    </div>
  `,
  styles: [`
    .patient-list { display: flex; flex-direction: column; gap: 1rem; }
    .patient-list__header { display: flex; justify-content: space-between; align-items: center; }
    .patient-list__table { width: 100%; border-collapse: collapse; }
    .patient-list__table th { text-align: left; padding: 0.5rem 0.75rem; background: var(--p-surface-100); cursor: pointer; user-select: none; }
    .patient-list__table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid var(--p-surface-200); }
    .patient-list__row { cursor: pointer; }
    .patient-list__row:hover td { background: var(--p-surface-50); }
    .patient-list__empty { text-align: center; color: var(--p-text-muted-color); padding: 2rem; }
    .patient-list__footer { display: flex; justify-content: space-between; align-items: center; }
  `],
})
export class PatientListComponent implements OnInit {
  protected readonly store = inject(PatientStore);
  private readonly authStore = inject(AuthStore);
  protected readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly searchControl = new FormControl('');

  readonly canRegister = computed(() => {
    const roles = this.authStore.roles();
    return roles.includes('CLERK') || roles.includes('ADMIN');
  });

  ngOnInit(): void {
    this.store.loadPatients();
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(term => this.store.setSearch(term ?? ''));
  }

  sortIndicator(field: string): string {
    if (this.store.sortField() !== field) return '';
    return this.store.sortDirection() === 'ASC' ? '↑' : '↓';
  }

  navigateToDetail(patient: Patient): void {
    this.router.navigate(['/patients', patient.id]);
  }

  onPageChange(event: PaginatorState): void {
    this.store.goToPage(event.page ?? 0);
  }
}
