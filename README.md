# Le-Games-App

## Tożsamość gracza i konto

Postęp gry jest przypisany do stałego UID gracza, a nie bezpośrednio do konta.

1. `POST /api/auth/begin` tworzy tożsamość gościa i zwraca `uid`. Klient powinien
   zachować ten UID.
2. Gość loguje się przez `POST /api/auth/login` z `{ "uid": "..." }`.
3. Aby utworzyć pełne konto i zachować dotychczasowy postęp, klient wysyła
   `POST /api/auth/register` z `login`, `email`, `password` i dotychczasowym
   `uid`. Rejestracja łączy konto z tą samą tożsamością. Pominięcie `uid`
   tworzy nową tożsamość dla konta.
4. Po połączeniu konta logowanie po UID jest wyłączone. Należy logować się
   przez `login` lub `email` razem z hasłem; token konta nadal wskazuje na
   dotychczasowy UID i daje dostęp do przypisanego do niego postępu.

Żądania do `/api/domino/games`, `/api/domino/game/{gameId}`,
`/api/domino/newgame` i `/api/domino/move` wymagają tokenu w nagłówku
`Authorization: Bearer <token>`. Można nimi zarządzać wyłącznie grami
przypisanymi do tożsamości z tokenu. UID gościa działa jak dane dostępowe —
należy go przechowywać prywatnie.