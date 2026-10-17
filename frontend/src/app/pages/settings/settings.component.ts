import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="account-page">
      <section class="account-card">
        <p class="eyebrow">PROFIL GRACZA</p>
        <h1>Ustawienia konta</h1>

        <p class="message" *ngIf="loadingUid">Przygotowuję tożsamość gracza…</p>
        <p class="message success" *ngIf="success">{{ success }}</p>
        <p class="message error" *ngIf="error">{{ error }}</p>

        <ng-container *ngIf="!linked; else linkedAccount">
          <h2>Zapisz postęp na koncie</h2>
          <form [formGroup]="registerForm" (ngSubmit)="registerAccount()">
            <label>
              Login
              <input formControlName="login" autocomplete="username" />
            </label>
            <label>
              Email
              <input type="email" formControlName="email" autocomplete="email" />
            </label>
            <label>
              Hasło
              <input type="password" formControlName="password" autocomplete="new-password" />
            </label>
            <button type="submit" [disabled]="busy || loadingUid || registerForm.invalid || !uid">
              {{ busy ? 'Tworzenie konta…' : 'Utwórz konto i zachowaj postęp' }}
            </button>
          </form>
          <p class="footnote">
            Masz już konto? <a routerLink="/login">Zaloguj się loginem lub emailem i hasłem.</a>
          </p>
        </ng-container>

        <ng-template #linkedAccount>
          <div class="linked-panel">
            <h2>Konto jest połączone</h2>
            <p>
              Loguj się za pomocą loginu lub adresu email i hasła. UID nie zmienia się, a
              dotychczasowy postęp pozostaje przypisany do tego gracza.
            </p>
            <a routerLink="/login">Przejdź do logowania</a>
          </div>
        </ng-template>
      </section>
    </main>
  `,
  styles: [
    `
      .account-page {
        width: min(760px, calc(100% - 32px));
        margin: 0 auto;
        padding: 42px 0 64px;
      }
      .account-card {
        padding: clamp(20px, 5vw, 38px);
        border: 1px solid var(--border);
        border-radius: 17px;
        background: var(--panel);
      }
      .eyebrow {
        margin: 0 0 10px;
        color: var(--accent);
        font-size: 0.72rem;
        font-weight: 750;
        letter-spacing: 0.12em;
      }
      h1,
      h2 {
        color: var(--text);
      }
      h1 {
        margin: 0;
        font-size: clamp(1.7rem, 4vw, 2.3rem);
      }
      h2 {
        margin: 28px 0 16px;
        font-size: 1.1rem;
      }
      .description,
      .footnote,
      .linked-panel p {
        color: var(--muted);
        line-height: 1.6;
      }
      .uid-panel,
      .linked-panel {
        display: grid;
        gap: 9px;
        margin: 24px 0;
        padding: 17px;
        border: 1px solid var(--border);
        border-radius: 12px;
        background: #111a1e;
      }
      .uid-panel span,
      .uid-panel small {
        color: var(--muted);
      }
      .uid-panel code {
        overflow-wrap: anywhere;
        color: var(--accent);
        font-size: 0.95rem;
      }
      .uid-panel small {
        font-size: 0.76rem;
      }
      form {
        display: grid;
        gap: 15px;
      }
      label {
        display: grid;
        gap: 7px;
        color: var(--text);
        font-size: 0.84rem;
        font-weight: 650;
      }
      input {
        box-sizing: border-box;
        width: 100%;
        padding: 12px;
        border: 1px solid #34464b;
        border-radius: 9px;
        background: #111a1e;
        color: var(--text);
        font: inherit;
      }
      input:focus {
        border-color: var(--accent);
        outline: none;
      }
      button {
        margin-top: 5px;
        padding: 13px 16px;
        border: 0;
        border-radius: 9px;
        background: var(--accent);
        color: #10201f;
        font-weight: 750;
        cursor: pointer;
      }
      button:disabled {
        cursor: not-allowed;
        opacity: 0.5;
      }
      a {
        color: var(--accent);
      }
      .message {
        margin: 15px 0;
        color: var(--muted);
      }
      .success {
        color: var(--accent);
      }
      .error {
        color: #ff9a9a;
      }
      .footnote {
        margin: 18px 0 0;
        font-size: 0.84rem;
      }
      .linked-panel {
        background: rgba(100, 216, 190, 0.05);
      }
      .linked-panel h2 {
        margin: 0;
      }
      .linked-panel p {
        margin: 0;
      }
    `,
  ],
})
export class SettingsComponent implements OnInit {
  readonly registerForm: FormGroup<{
    login: FormControl<string>;
    email: FormControl<string>;
    password: FormControl<string>;
  }>;

  uid: string | null;
  linked: boolean;
  loadingUid = false;
  busy = false;
  error = '';
  success = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
  ) {
    this.registerForm = this.formBuilder.nonNullable.group({
      login: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
    this.uid = this.authService.getPlayerUid();
    this.linked = this.authService.isAccountSession();
  }

  ngOnInit(): void {
    if (this.uid) return;
    this.loadingUid = true;
    this.authService.ensurePlayerUid().subscribe({
      next: (uid) => {
        this.uid = uid;
        this.loadingUid = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Nie udało się utworzyć tożsamości gracza.';
        this.loadingUid = false;
      },
    });
  }

  registerAccount(): void {
    if (this.busy || this.registerForm.invalid) return;
    const { login, email, password } = this.registerForm.getRawValue();
    this.busy = true;
    this.error = '';
    this.success = '';

    this.authService.registerAccount(login, email, password).subscribe({
      next: () => {
        this.linked = true;
        this.uid = this.authService.getPlayerUid();
        this.success = 'Konto utworzone. Twój dotychczasowy postęp został zachowany.';
        this.busy = false;
      },
      error: (err) => {
        this.error = err?.error?.detail ?? err?.error?.message ?? 'Nie udało się utworzyć konta.';
        this.busy = false;
      },
    });
  }
}
