import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { GuessCityRound, GuessCitySuggestionsResponse } from '../dto/guess-city';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class GuessCityService {
  private readonly API_ADDRESS = `${environment.apiUrl}/guess-city`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  start(): Observable<GuessCityRound> {
    return this.withAuthorization(token =>
      this.http.post<GuessCityRound>(
        `${this.API_ADDRESS}/start`,
        {},
        { headers: this.buildHeaders(token) }
      )
    );
  }

  submitGuess(gameId: number, cityName: string): Observable<GuessCityRound> {
    return this.withAuthorization(token =>
      this.http.post<GuessCityRound>(
        `${this.API_ADDRESS}/guess`,
        { gameId, cityName },
        { headers: this.buildHeaders(token) }
      )
    );
  }

  getSuggestions(input: string): Observable<string[]> {
    const params = new HttpParams().set('text', input);
    return this.withAuthorization(token =>
      this.http.get<GuessCitySuggestionsResponse>(
        `${this.API_ADDRESS}/suggestions`,
        { params, headers: this.buildHeaders(token) }
      )
    ).pipe(map(response => response.suggestions ?? []));
  }

  private buildHeaders(token: string): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  private withAuthorization<T>(request: (token: string) => Observable<T>): Observable<T> {
    return this.authService.getToken().pipe(
      switchMap(token => request(token).pipe(
        catchError(error => {
          if (error.status !== 401) {
            return throwError(() => error);
          }
          return this.authService.reauthenticateAfterUnauthorized(token).pipe(
            switchMap(refreshedToken => request(refreshedToken))
          );
        })
      ))
    );
  }
}
