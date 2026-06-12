import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PrescriptionStore } from '../prescription.store';
import { PrescriptionResource, PrescriptionStatus } from '../prescription.model';

@Component({
  selector: 'app-pharmacist-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule, TableModule, TagModule, SkeletonModule, ButtonModule, DialogModule, InputTextModule],
  template: `
    <div class="pharmacist-dashboard">
      <h2>Prescriptions</h2>

      @if (store.loading()) {
        <p-skeleton height="2rem" styleClass="mb-2" />
        <p-skeleton height="2rem" styleClass="mb-2" />
        <p-skeleton height="2rem" />
      } @else {
        <p-table [value]="store.prescriptions()" [tableStyle]="{'min-width': '50rem'}">
          <ng-template pTemplate="header">
            <tr>
              <th>Patient</th>
              <th>Doctor</th>
              <th>Medications</th>
              <th>Status</th>
              <th>Issued</th>
              <th></th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-rx>
            <tr>
              <td>{{ rx.patientName }}</td>
              <td>{{ rx.doctorName }}</td>
              <td>{{ rx.medications.length }} item(s)</td>
              <td>
                <p-tag [value]="rx.status" [severity]="statusSeverity(rx.status)" />
              </td>
              <td>{{ rx.issuedAt | date:'short' }}</td>
              <td>
                @if (rx._links?.['fulfill-prescription']) {
                  <p-button label="Dispense" size="small"
                            (click)="store.fulfill(rx)"
                            [disabled]="store.loading()" />
                }
                @if (rx._links?.['cancel-prescription']) {
                  <p-button label="Cancel" size="small" severity="danger"
                            (click)="onCancel(rx)"
                            [disabled]="store.loading()" />
                }
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="6" class="pharmacist-dashboard__empty">No prescriptions found.</td>
            </tr>
          </ng-template>
        </p-table>
      }

      <p-dialog
        header="Cancel Prescription"
        [visible]="cancelDialogVisible()"
        (visibleChange)="cancelDialogVisible.set($event)"
        [modal]="true"
        [closable]="true"
        [style]="{'min-width': '25rem'}">
        <div class="pharmacist-dashboard__cancel-form">
          <label for="cancel-reason">Cancellation reason</label>
          <input
            id="cancel-reason"
            pInputText
            type="text"
            placeholder="Enter reason"
            [ngModel]="cancelReason()"
            (ngModelChange)="cancelReason.set($event)" />
        </div>
        <ng-template pTemplate="footer">
          <p-button label="Close" severity="secondary" (click)="dismissCancel()" />
          <p-button label="Confirm" [disabled]="!cancelReason().trim()" (click)="submitCancel()" />
        </ng-template>
      </p-dialog>
    </div>
  `,
  styles: [`
    .pharmacist-dashboard { display: flex; flex-direction: column; gap: 1rem; }
    .pharmacist-dashboard__empty { text-align: center; color: var(--p-text-muted-color); padding: 2rem; }
    .pharmacist-dashboard__cancel-form { display: flex; flex-direction: column; gap: 0.5rem; padding: 0.5rem 0; }
  `],
})
export class PharmacistDashboardComponent implements OnInit {
  protected readonly store = inject(PrescriptionStore);

  protected readonly cancelDialogVisible = signal(false);
  protected readonly cancelReason = signal('');
  private readonly rxToCancel = signal<PrescriptionResource | null>(null);

  ngOnInit(): void {
    this.store.loadPrescriptions();
  }

  protected onCancel(rx: PrescriptionResource): void {
    this.rxToCancel.set(rx);
    this.cancelReason.set('');
    this.cancelDialogVisible.set(true);
  }

  protected async submitCancel(): Promise<void> {
    const rx = this.rxToCancel();
    const reason = this.cancelReason().trim();
    if (!rx || !reason) return;
    const success = await this.store.cancel(rx, reason);
    if (success) {
      this.cancelDialogVisible.set(false);
      this.rxToCancel.set(null);
      this.cancelReason.set('');
    }
  }

  protected dismissCancel(): void {
    this.cancelDialogVisible.set(false);
    this.rxToCancel.set(null);
    this.cancelReason.set('');
  }

  protected statusSeverity(status: PrescriptionStatus): 'success' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case 'FULFILLED': return 'success';
      case 'EXPIRED': return 'warn';
      case 'CANCELLED': return 'danger';
      default: return 'secondary';
    }
  }
}
