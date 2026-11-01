export interface GuessCityGuess {
  cityName: string;
  distanceKm: number;
}

export interface GuessCityRound {
  id: number;
  date: string;
  inProgress: boolean;
  cityAsciiName: string | null;
  kmlFileBase64: string;
  guesses: GuessCityGuess[];
}

export interface GuessCitySuggestionsResponse {
  suggestions: string[];
}
