export interface BeginResponse {
  uid: string;
}

export interface Position {
  x: number;
  y: number;
}

export interface Obstacle {
  x: number;
  y: number;
}

export interface Piece {
  pos: Position;
  direction: string;
  cityName: string;
}

export interface GameResponse {
  obstacles: Obstacle[];
  pieces: Piece[];
  complete: boolean;
  gameId: number;
}

export interface UserProfile {
  username: string;
  email: string;
}