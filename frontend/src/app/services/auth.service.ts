import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CookieService } from './cookie.service';
import { environment } from '../../environments/environment';
import { map } from 'rxjs/operators';
import { of, Observable } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly API_ADDRESS =
    `${environment.apiUrl}/auth`;

  constructor(
    private http: HttpClient,
    private cookieService: CookieService
  ) {}

  saveToken(): Observable<string> {
    const uid = this.cookieService.getCookie("UID");

    return this.http.post<LoginResponse>(
      `${this.API_ADDRESS}/login`,
      { uid },
      { withCredentials: true }
    ).pipe(
      map(response => {
        this.cookieService.setCookie("token", response.token);
        return response.token;
      })
    );
  }

  getToken(): Observable<string> {
    const token = this.cookieService.getCookie('token');

    if (!token || !this.isTokenValid(token)) {
      return this.saveToken();
    }

    return of(token);
  }

  isTokenValid(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const now = Math.floor(Date.now() / 1000);

      return payload.exp && payload.exp > now;
    } catch {
      return false;
    }
  }

  initializeUser() {
    const uid = this.cookieService.getCookie("UID");

    if (uid) {
      console.log("UID exists:", uid);
      return;
    }

    this.http.post<BeginResponse>(
      `${this.API_ADDRESS}/begin`,
      {},
      {
        withCredentials: true
      }
    ).subscribe({
      next: (response) => {
        this.cookieService.setCookie(
          "UID",
          response.uid
        );

        console.log("UID saved:", response.uid);
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
}

interface BeginResponse {
  uid: string;
}

interface LoginResponse {
  token: string;
}