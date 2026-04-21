import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PanelMenuModule } from 'primeng/panelmenu';
import { MenuItem } from 'primeng/api';
import { AuthStore } from '../auth/auth.store';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, PanelMenuModule],
  template: `
    <div class="shell-layout">
      <nav class="shell-sidebar">
        <div class="shell-sidebar__logo">⚕ QuantumHealth</div>
        <p-panelMenu [model]="menuItems()" styleClass="shell-sidebar__menu" />
      </nav>
      <main class="shell-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .shell-layout { display: flex; height: 100dvh; }
    .shell-sidebar { width: 240px; border-right: 1px solid var(--p-surface-border); display: flex; flex-direction: column; }
    .shell-sidebar__logo { padding: 1rem; font-weight: 700; font-size: 1rem; border-bottom: 1px solid var(--p-surface-border); }
    .shell-content { flex: 1; overflow-y: auto; padding: 1.5rem; }
    :host ::ng-deep .shell-sidebar__menu { border: none; width: 100%; }
  `],
})
export class ShellComponent {
  private readonly authStore = inject(AuthStore);

  readonly menuItems = computed<MenuItem[]>(() => {
    const roles = this.authStore.roles();
    const adminItems: MenuItem[] = [];
    if (roles.includes('PHARMACIST')) {
      adminItems.push({ label: 'Pharmacy', icon: 'pi pi-box', routerLink: ['/pharmacy'] });
    }
    if (roles.includes('BILLING_CLERK')) {
      adminItems.push({ label: 'Billing', icon: 'pi pi-file-invoice', routerLink: ['/billing'] });
    }
    if (roles.includes('ADMIN')) {
      adminItems.push({ label: 'Audit', icon: 'pi pi-list', routerLink: ['/audit'] });
    }
    return [
      {
        label: 'Clinical',
        expanded: true,
        items: [
          { label: 'Patients', icon: 'pi pi-users', routerLink: ['/patients'] },
          { label: 'Appointments', icon: 'pi pi-calendar', routerLink: ['/appointments'] },
          { label: 'Encounters', icon: 'pi pi-heart', routerLink: ['/encounters'] },
          { label: 'Lab', icon: 'pi pi-flask', routerLink: ['/lab'] },
        ],
      },
      ...(adminItems.length > 0 ? [{ label: 'Admin', expanded: true, items: adminItems }] : []),
    ];
  });
}
