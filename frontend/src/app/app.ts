import { Component, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { GlobalErrorService } from './core/global-error.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastModule],
  providers: [MessageService],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly errorService = inject(GlobalErrorService);
  private readonly messageService = inject(MessageService);

  constructor() {
    effect(() => {
      const err = this.errorService.lastError();
      if (err) {
        this.messageService.add({ severity: 'error', summary: err.summary, detail: err.detail, life: 5000 });
        this.errorService.clear();
      }
    });
  }
}
