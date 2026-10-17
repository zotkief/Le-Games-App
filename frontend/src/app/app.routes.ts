import { Routes } from '@angular/router';

import { HomeComponent } from './pages/home/home.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { DominoComponent } from './pages/domino/domino.component';
import { Page2Component } from './pages/page2/page2.component';
import { LoginComponent } from './login.component';

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
    path: 'login',
    component: LoginComponent
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