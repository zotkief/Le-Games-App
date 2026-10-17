import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CookieService } from './cookie.service';
import { environment } from '../../environments/environment';
import { Observable, map, of, shareReplay, tap, throwError, finalize, switchMap } from 'rxjs';

interface BeginResponse {
  uid: string;
}

interface LoginResponse {
  token: string;
}

interface RegisterResponse {
  uid: string;
}

interface TokenPayload {
  uid?: string;
  authType?: 'GUEST' | 'ACCOUNT';
  exp?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_ADDRESS = `${environment.apiUrl}/auth`;
  private uidInitialization?: Observable<string>;

  constructor(
    private http: HttpClient,
    private cookieService: CookieService
  ) {}

  getPlayerUid(): string | null {
    return this.cookieService.getCookie('UID');
  }

  isAccountSession(): boolean {
    const token = this.cookieService.getCookie('token');
    const payload = token ? this.readTokenPayload(token) : null;
    return payload?.authType === 'ACCOUNT';
  }

  ensurePlayerUid(): Observable<string> {
    const currentUid = this.getPlayerUid();
    if (currentUid) return of(currentUid);
    if (this.uidInitialization) return this.uidInitialization;

    this.uidInitialization = this.http.post<BeginResponse>(
      `${this.API_ADDRESS}/begin`,
      {},
      { withCredentials: true }
    ).pipe(
      map(response => {
        this.cookieService.setCookie('UID', response.uid);
        return response.uid;
      }),
      finalize(() => {
        this.uidInitialization = undefined;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    return this.uidInitialization;
  }

  getToken(): Observable<string> {
    const token = this.cookieService.getCookie('token');
    const payload = token ? this.readTokenPayload(token) : null;

    if (token && payload && this.isTokenValid(token)
        && payload.uid === this.getPlayerUid()) {
      return of(token);
    }

    if (payload?.authType === 'ACCOUNT') {
      return throwError(() => new Error('Zaloguj się ponownie loginem lub adresem email i hasłem.'));
    }

    return this.ensurePlayerUid().pipe(
      switchMap(uid => this.loginWithUid(uid))
    );
  }

  loginWithUid(uid: string): Observable<string> {
    return this.storeLogin({ uid });
  }

  reauthenticateAfterUnauthorized(failedToken: string): Observable<string> {
    if (this.cookieService.getCookie('token') !== failedToken) {
      return this.getToken();
    }

    const payload = this.readTokenPayload(failedToken);
    this.cookieService.removeCookie('token');

    if (payload?.authType === 'ACCOUNT') {
      return throwError(() => new Error(
        'Sesja wygasła. Zaloguj się ponownie loginem lub adresem email i hasłem.'
      ));
    }

    const uid = payload?.uid ?? this.getPlayerUid();
    if (!uid) {
      return this.ensurePlayerUid().pipe(
        switchMap(playerUid => this.loginWithUid(playerUid))
      );
    }

    return this.loginWithUid(uid);
  }

  loginWithCredentials(loginOrEmail: string, password: string): Observable<string> {
    const credential = loginOrEmail.trim();
    const body = credential.includes('@')
      ? { email: credential, password }
      : { login: credential, password };

    return this.storeLogin(body);
  }

  registerAccount(login: string, email: string, password: string): Observable<string> {
    return this.ensurePlayerUid().pipe(
      switchMap(uid => this.http.post<RegisterResponse>(
        `${this.API_ADDRESS}/register`,
        { uid, login: login.trim(), email: email.trim(), password }
      )),
      tap(response => {
        this.cookieService.setCookie('UID', response.uid);
        this.cookieService.removeCookie('token');
      }),
      switchMap(() => this.loginWithCredentials(login, password))
    );
  }

  private storeLogin(credentials: object): Observable<string> {
    return this.http.post<LoginResponse>(
      `${this.API_ADDRESS}/login`,
      credentials,
      { withCredentials: true }
    ).pipe(
      map(response => {
        const payload = this.readTokenPayload(response.token);
        if (!payload?.uid) {
          throw new Error('Serwer zwrócił nieprawidłowy token logowania.');
        }
        this.cookieService.setCookie('UID', payload.uid);
        this.cookieService.setCookie('token', response.token);
        return response.token;
      })
    );
  }

  private isTokenValid(token: string): boolean {
    const payload = this.readTokenPayload(token);
    if (!payload?.uid || !payload.authType) return false;
    return payload.exp === undefined || payload.exp > Math.floor(Date.now() / 1000);
  }

  private readTokenPayload(token: string): TokenPayload | null {
    try {
      const encodedPayload = token.split('.')[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(token.split('.')[1].length / 4) * 4, '=');
      return JSON.parse(atob(encodedPayload)) as TokenPayload;
    } catch {
      return null;
    }
  }
}
