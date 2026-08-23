import { AsyncPipe, CommonModule, NgIf } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIf, AsyncPipe],
  template: `
    <main class="login-page">
      <section class="login-card">
        <h1>Logowanie</h1>
        <form [formGroup]="loginForm" (ngSubmit)="submit()">
          <label>
            Login lub email
            <input formControlName="loginOrEmail" placeholder="login lub email" />
          </label>

          <label>
            Hasło
            <input type="password" formControlName="password" placeholder="hasło" />
          </label>

          <p class="divider">lub</p>

          <label>
            UID gracza
            <input formControlName="uid" placeholder="uid gracza" />
          </label>

          <button type="submit" [disabled]="!canSubmit()">Zaloguj</button>
        </form>

        <div class="message success" *ngIf="token()">Token: {{ token() }}</div>
        <div class="message error" *ngIf="error()">Błąd: {{ error() }}</div>
      </section>
    </main>
  `,
  styles: [
    `
      .login-page {
        min-height: 100vh;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #f5f7fb;
        padding: 2rem;
      }
      .login-card {
        width: min(100%, 420px);
        background: white;
        border-radius: 1rem;
        box-shadow: 0 24px 60px rgba(0, 0, 0, 0.08);
        padding: 2rem;
      }
      h1 {
        margin: 0 0 1.5rem;
        font-size: 1.75rem;
      }
      label {
        display: grid;
        gap: 0.5rem;
        margin-bottom: 1rem;
        font-size: 0.95rem;
      }
      input {
        width: 100%;
        border: 1px solid #d2d8e2;
        border-radius: 0.75rem;
        padding: 0.9rem 1rem;
        font-size: 1rem;
        outline: none;
        transition: border-color 0.2s ease;
      }
      input:focus {
        border-color: #4f46e5;
      }
      .divider {
        margin: 1rem 0;
        text-align: center;
        color: #6b7280;
      }
      button {
        width: 100%;
        padding: 0.95rem 1rem;
        border: none;
        border-radius: 0.85rem;
        background: #4f46e5;
        color: white;
        font-weight: 600;
        cursor: pointer;
      }
      button:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      .message {
        margin-top: 1rem;
        padding: 0.95rem 1rem;
        border-radius: 0.75rem;
        word-break: break-word;
      }
      .success {
        background: #ecfdf5;
        color: #065f46;
      }
      .error {
        background: #fef2f2;
        color: #991b1b;
      }
    `
  ]
})
export class LoginComponent {
  token = signal('');
  error = signal('');

  loginForm!: any;

  constructor(private fb: FormBuilder, private http: HttpClient) {
      this.loginForm = this.fb.group({
          loginOrEmail: [''],
          password: ['']
      });
  }

  canSubmit(): boolean {
    const loginOrEmail = this.loginForm.get('loginOrEmail')?.value?.trim();
    const password = this.loginForm.get('password')?.value?.trim();
    const uid = this.loginForm.get('uid')?.value?.trim();
    return (loginOrEmail && password) || uid;
  }

  submit(): void {
    this.token.set('');
    this.error.set('');

    const loginOrEmail = this.loginForm.get('loginOrEmail')?.value?.trim();
    const password = this.loginForm.get('password')?.value?.trim();
    const uid = this.loginForm.get('uid')?.value?.trim();

    const body: any = {};
    if (uid) {
      body.uid = uid;
    }
    if (loginOrEmail) {
      if (loginOrEmail.includes('@')) {
        body.email = loginOrEmail;
      } else {
        body.login = loginOrEmail;
      }
    }
    if (password) {
      body.password = password;
    }

    this.http.post<{ token: string }>('/api/auth/login', body)
      .subscribe({
        next: (result) => this.token.set(result.token),
        error: (err) => this.error.set(err?.error?.message || 'Nieprawidłowe dane logowania')
      });
  }
}
