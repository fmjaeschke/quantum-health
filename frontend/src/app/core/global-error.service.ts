import { Injectable, Signal, signal } from '@angular/core';

export interface AppError {
  summary: string;
  detail: string;
}

@Injectable({ providedIn: 'root' })
export class GlobalErrorService {
  private readonly _error = signal<AppError | null>(null);
  readonly lastError: Signal<AppError | null> = this._error.asReadonly();

  show(summary: string, detail: string): void {
    this._error.set({ summary, detail });
  }

  clear(): void {
    this._error.set(null);
  }
}
