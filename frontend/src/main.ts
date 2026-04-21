import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';

fetch('/assets/config.json')
  .then(r => r.json())
  .then(cfg => bootstrapApplication(App, appConfig(cfg)))
  .catch(err => console.error('Failed to load runtime config', err));
