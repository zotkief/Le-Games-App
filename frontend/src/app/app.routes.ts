import { Routes } from '@angular/router';

import { HomeComponent } from './pages/home/home.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { DominoComponent } from './pages/domino/domino.component';
import { Page2Component } from './pages/page2/page2.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'settings',
    component: SettingsComponent
  },
  {
    path: 'domino',
    component: DominoComponent
  },
  {
    path: 'page2',
    component: Page2Component
  }
];