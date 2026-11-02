import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import type { Map as MapLibreMap } from 'maplibre-gl';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  of,
  switchMap,
  takeUntil,
} from 'rxjs';
import { GuessCityRound } from '../../dto/guess-city';
import { GuessCityService } from '../../services/guess-city.service';

interface RoutePoint {
  longitude: number;
  latitude: number;
}

@Component({
  selector: 'app-guess-city',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './guess-city.component.html',
  styleUrl: './guess-city.component.css',
})
export class GuessCityComponent implements AfterViewInit, OnInit, OnDestroy {
  readonly searchControl = new FormControl('');

  @ViewChild('mapElement') private mapElement?: ElementRef<HTMLDivElement>;

  round?: GuessCityRound;
  suggestions: string[] = [];
  showSuggestions = false;
  loading = false;
  submitting = false;
  errorMessage = '';
  mapErrorMessage = '';
  successMessage = '';
  routeCoordinates: RoutePoint[][] = [];
  mapLoaded = false;

  private readonly destroy$ = new Subject<void>();
  private map?: MapLibreMap;
  private viewInitialized = false;
  private mapInitializationStarted = false;
  private destroyed = false;

  constructor(
    private readonly guessCityService: GuessCityService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.startGame();
    this.searchControl.valueChanges
      .pipe(
        debounceTime(200),
        distinctUntilChanged(),
        switchMap((value) => {
          const query = (value ?? '').trim();
          if (!query) {
            this.suggestions = [];
            return of([]);
          }
          return this.guessCityService.getSuggestions(query).pipe(
            catchError((error) => {
              this.showError(error);
              return of([]);
            }),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((options) => {
        this.suggestions = options;
        this.showSuggestions = options.length > 0;
        this.cdr.detectChanges();
      });
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.initializeMap();
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.destroy$.next();
    this.destroy$.complete();
    this.map?.remove();
  }

  startGame(): void {
    this.loading = true;
    this.errorMessage = '';
    this.guessCityService.start().subscribe({
      next: (round) => {
        this.round = round;
        this.renderKml(round.kmlFileBase64);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.showError(error);
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  selectSuggestion(city: string): void {
    this.searchControl.setValue(city);
    this.suggestions = [];
    this.showSuggestions = false;
  }

  hideSuggestions(): void {
    setTimeout(() => {
      this.showSuggestions = false;
    }, 120);
  }

  submitGuess(): void {
    const cityName = (this.searchControl.value ?? '').trim();
    if (!cityName || !this.round?.inProgress || this.submitting) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.guessCityService.submitGuess(this.round.id, cityName).subscribe({
      next: (round) => {
        this.round = round;
        this.searchControl.setValue('');
        this.suggestions = [];
        this.showSuggestions = false;
        this.submitting = false;
        if (!round.inProgress) {
          this.successMessage = 'Brawo! Odgadłeś dzisiejsze miasto.';
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.showError(error);
        this.submitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  private renderKml(kmlBase64: string): void {
    try {
      const kml = atob(kmlBase64);
      const document = new DOMParser().parseFromString(kml, 'application/xml');
      if (document.querySelector('parsererror')) {
        throw new Error('Nie udało się odczytać geometrii mapy.');
      }

      const coordinates = Array.from(document.getElementsByTagNameNS('*', 'coordinates'))
        .map((element) => element.textContent ?? '')
        .map((value) =>
          value
            .trim()
            .split(/\s+/)
            .map((pair) => {
              const [longitude, latitude] = pair.split(',').map(Number);
              return { longitude, latitude };
            })
            .filter(
              (point) =>
                Number.isFinite(point.longitude) &&
                Number.isFinite(point.latitude) &&
                Math.abs(point.latitude) <= 85.05112878,
            ),
        )
        .filter((points) => points.length > 1);

      if (coordinates.length === 0) {
        this.routeCoordinates = [];
        return;
      }

      this.routeCoordinates = coordinates;
      this.initializeMap();
    } catch (error) {
      this.routeCoordinates = [];
      this.errorMessage = error instanceof Error ? error.message : 'Nie udało się wyświetlić mapy.';
    }
  }

  private initializeMap(): void {
    if (
      !this.viewInitialized ||
      !this.mapElement ||
      !this.routeCoordinates.length ||
      this.mapInitializationStarted
    ) {
      return;
    }

    this.mapInitializationStarted = true;
    void this.loadMap();
  }

  private async loadMap(): Promise<void> {
    try {
      const [{ Map: MapLibre, setWorkerUrl }, { LogoControl }] = await Promise.all([
        import('maplibre-gl'),
        import('@maptoolkit/maplibre-logo-control'),
      ]);
      if (this.destroyed || !this.mapElement) {
        return;
      }

      setWorkerUrl(new URL('assets/maplibre-gl/maplibre-gl-worker.mjs', document.baseURI).href);
      const bounds = this.getRouteBounds();
      this.map = new MapLibre({
        container: this.mapElement.nativeElement,
        style: 'https://styles.maptoolkit.org/light.json',
        center: [(bounds[0][0] + bounds[1][0]) / 2, (bounds[0][1] + bounds[1][1]) / 2],
        zoom: 10,
        attributionControl: { compact: false },
        transformStyle: (_previousStyle, nextStyle) => ({
          ...nextStyle,
          layers: nextStyle.layers.filter((layer) => layer.type !== 'symbol'),
        }),
      });
      this.map.addControl(new LogoControl(), 'bottom-left');
      this.map.on('load', () => {
        this.map?.resize();
        this.mapLoaded = true;
        this.mapErrorMessage = '';
        this.addRoutesToMap();
        this.fitRouteBounds();
        this.lockMapInteractions();
        this.cdr.detectChanges();
      });
      this.map.on('resize', () => this.fitRouteBounds());
      this.map.on('error', () => {
        if (!this.mapLoaded) {
          this.mapErrorMessage = 'Nie udało się załadować mapy Maptoolkit.';
          this.cdr.detectChanges();
        }
      });
    } catch (error) {
      if (!this.destroyed) {
        this.mapErrorMessage =
          error instanceof Error ? error.message : 'Nie udało się załadować mapy Maptoolkit.';
        this.cdr.detectChanges();
      }
    }
  }

  private addRoutesToMap(): void {
    if (!this.map || !this.routeCoordinates.length) {
      return;
    }

    const features = this.routeCoordinates.map((line) => ({
      type: 'Feature' as const,
      properties: {},
      geometry: {
        type: 'LineString' as const,
        coordinates: line.map((point) => [point.longitude, point.latitude]),
      },
    }));

    this.map.addSource('tram-routes', {
      type: 'geojson',
      data: { type: 'FeatureCollection', features },
    });
    this.map.addLayer({
      id: 'tram-routes',
      type: 'line',
      source: 'tram-routes',
      layout: {
        'line-join': 'round',
        'line-cap': 'round',
      },
      paint: {
        'line-color': '#d33c34',
        'line-width': 1,
        'line-opacity': 0.9,
      },
    });
  }

  private getRouteBounds(): [[number, number], [number, number]] {
    const points = this.routeCoordinates.flat();
    const bounds = points.reduce(
      (current, point) => ({
        west: Math.min(current.west, point.longitude),
        south: Math.min(current.south, point.latitude),
        east: Math.max(current.east, point.longitude),
        north: Math.max(current.north, point.latitude),
      }),
      {
        west: Infinity,
        south: Infinity,
        east: -Infinity,
        north: -Infinity,
      },
    );

    return [
      [bounds.west, bounds.south],
      [bounds.east, bounds.north],
    ];
  }

  private fitRouteBounds(): void {
    if (!this.map || !this.routeCoordinates.length) {
      return;
    }

    this.map.fitBounds(this.getRouteBounds(), {
      padding: 24,
      maxZoom: 16,
      duration: 0,
    });
  }

  private lockMapInteractions(): void {
    if (!this.map) {
      return;
    }

    this.map.scrollZoom.disable();
    this.map.boxZoom.disable();
    this.map.dragRotate.disable();
    this.map.dragPan.disable();
    this.map.keyboard.disable();
    this.map.doubleClickZoom.disable();
    this.map.touchZoomRotate.disable();
    this.map.touchPitch.disable();
  }

  private showError(error: {
    status?: number;
    error?: { message?: string };
    message?: string;
  }): void {
    if (error.status === 404) {
      this.errorMessage = 'Dzisiejsza gra nie jest jeszcze dostępna. Spróbuj ponownie później.';
    } else if (error.status === 401) {
      this.errorMessage = 'Sesja wygasła. Zaloguj się ponownie.';
    } else {
      this.errorMessage =
        error.error?.message ?? error.message ?? 'Wystąpił błąd. Spróbuj ponownie.';
    }
  }
}
