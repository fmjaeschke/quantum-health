import { Injectable, Signal, computed, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class HateoasService {
  private readonly store = signal<Map<string, Map<string, string>>>(new Map());

  updateLinks(url: string, links: Record<string, { href: string }>): void {
    const parsed = new Map(
      Object.entries(links).map(([rel, obj]) => [rel, obj.href])
    );
    this.store.update(m => new Map(m).set(url, parsed));
  }

  can(url: string, rel: string): Signal<boolean> {
    return computed(() => this.store().get(url)?.has(rel) ?? false);
  }

  href(url: string, rel: string): Signal<string | undefined> {
    return computed(() => this.store().get(url)?.get(rel));
  }
}
