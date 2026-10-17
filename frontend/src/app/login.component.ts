import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="login-page">
      <section class="login-card">
        <h1>Logowanie</h1>
        <p>Zaloguj się na konto albo odzyskaj postęp gościa przez UID.</p>

        <form [formGroup]="loginForm" (ngSubmit)="submit()">
          <label>
            Login lub email
            <input formControlName="loginOrEmail" autocomplete="username" placeholder="login lub email" />
          </label>

          <label>
            Hasło
            <input type="password" formControlName="password" autocomplete="current-password" placeholder="hasło" />
          </label>

          <button type="submit" [disabled]="busy || !canSubmit()">Zaloguj na konto</button>
        </form>

        <p class="divider">lub zaloguj się jako gość</p>
        <form [formGroup]="guestForm" (ngSubmit)="loginAsGuest()">
          <label>
            UID gracza
            <input formControlName="uid" autocomplete="off" placeholder="uid gracza" />
          </label>
          <button type="submit" [disabled]="busy || !guestForm.controls.uid.value.trim()">
            Zaloguj przez UID
          </button>
        </form>

        <p class="message success" *ngIf="success">{{ success }}</p>
        <p class="message error" *ngIf="error">{{ error }}</p>
        <a routerLink="/settings">Utwórz konto i zachowaj postęp</a>
      </section>
    </main>
  `,
  styles: [`
    .login-page { min-height: 70vh; display: grid; place-items: center; padding: 2rem; }
    .login-card { width: min(100%, 440px); padding: 2rem; border: 1px solid var(--border); border-radius: 1rem; background: var(--panel); }
    h1 { margin: 0 0 .5rem; color: var(--text); }
    p { color: var(--muted); }
    form { display: grid; gap: 1rem; margin-top: 1.25rem; }
    label { display: grid; gap: .5rem; color: var(--text); font-size: .9rem; }
    input { box-sizing: border-box; width: 100%; padding: .8rem; border: 1px solid var(--border); border-radius: .65rem; background: #111a1e; color: var(--text); font: inherit; }
    button { padding: .85rem 1rem; border: 0; border-radius: .65rem; background: var(--accent); color: #10201f; font-weight: 700; cursor: pointer; }
    button:disabled { cursor: not-allowed; opacity: .55; }
    .divider { margin: 1.5rem 0 .5rem; text-align: center; }
    .message { margin: 1rem 0; }
    .success { color: var(--accent); }
    .error { color: #ff9a9a; }
    a { display: inline-block; margin-top: 1.25rem; color: var(--accent); }
  `]
})
export class LoginComponent {
  readonly loginForm: FormGroup<{
    loginOrEmail: FormControl<string>;
    password: FormControl<string>;
  }>;
  readonly guestForm: FormGroup<{ uid: FormControl<string> }>;

  busy = false;
  error = '';
  success = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService
  ) {
    this.loginForm = this.formBuilder.nonNullable.group({
      loginOrEmail: '',
      password: ''
    });
    this.guestForm = this.formBuilder.nonNullable.group({
      uid: this.authService.getPlayerUid() ?? ''
    });
  }

  canSubmit(): boolean {
    const { loginOrEmail, password } = this.loginForm.getRawValue();
    return loginOrEmail.trim().length > 0 && password.length > 0;
  }

  submit(): void {
    if (this.busy || !this.canSubmit()) return;
    const { loginOrEmail, password } = this.loginForm.getRawValue();
    this.authenticate(this.authService.loginWithCredentials(loginOrEmail, password));
  }

  loginAsGuest(): void {
    const uid = this.guestForm.controls.uid.value.trim();
    if (this.busy || !uid) return;
    this.authenticate(this.authService.loginWithUid(uid));
  }

  private authenticate(request: Observable<string>): void {
    this.busy = true;
    this.error = '';
    this.success = '';

    request.subscribe({
      next: () => {
        this.success = 'Zalogowano. Twój postęp jest dostępny na tym urządzeniu.';
        this.busy = false;
      },
      error: err => {
        this.error = err?.error?.message ?? err?.message ?? 'Nieprawidłowe dane logowania.';
        this.busy = false;
      }
    });
  }
}
