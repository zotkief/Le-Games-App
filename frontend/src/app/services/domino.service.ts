import { Injectable } from '@angular/core';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { GameResponse, Position } from '../dto/domino';
import { AuthService  } from './auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { map } from 'rxjs';
import { HttpParams } from '@angular/common/http';



@Injectable({
  providedIn: 'root'
})
export class GameService {


  private readonly API_ADDRESS =
    `${environment.apiUrl}/domino`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private buildHeaders(token: string): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  private withAuthorization<T>(
    request: (token: string) => Observable<T>
  ): Observable<T> {
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

  createGame(date?: string): Observable<GameResponse> {
    const body: { date?: string } = {};

    if (date) {
      body.date = date;
    }

    return this.withAuthorization(token =>
      this.http.post<GameResponse>(
        `${this.API_ADDRESS}/newgame`,
        body,
        {
          headers: this.buildHeaders(token)
        }
      )
    );
  }
  move(
    city: string,
    pos: Position,
    gameId: number,
    direction: string
  ): Observable<GameResponse> {

    const body = {
      x: pos.x,
      y: pos.y,
      gameId,
      direction,
      cityName: city
    };

    return this.withAuthorization(token =>
      this.http.post<GameResponse>(
        `${this.API_ADDRESS}/move`,
        body,
        {
          headers: this.buildHeaders(token)
        }
      )
    );
  }

  getSuggestions(input: string): Observable<string[]> {
    console.log("REQUEST:", input);

    const params = new HttpParams().set('text', input);

    return this.withAuthorization(token =>
      this.http.get<{ suggestions: string[] }>(
        `${this.API_ADDRESS}/suggestions`,
        {
          params,
          headers: this.buildHeaders(token)
        }
      )
    ).pipe(
      map(res => res.suggestions ?? [])
    );
  }
}



interface CreateGameRequest {
  date?: string;
}
