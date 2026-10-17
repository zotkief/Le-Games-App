import { Component, OnInit } from '@angular/core';
import { GameService } from '../../services/domino.service';
import { GameResponse, Obstacle, Piece } from '../../dto/domino';

import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';
import { FormControl } from '@angular/forms';
import { distinctUntilChanged } from 'rxjs/operators';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { of } from 'rxjs';
import { switchMap, filter, tap } from 'rxjs/operators';


type CellType =
  | 'EMPTY'
  | 'OBSTACLE'
  | 'PIECE';

type Cell = {
  image: string;
  type: CellType;
  piece?: Piece;
};

type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT';

type TextBox = {
  x: number;
  y: number;
  text: string;
  direction: Direction;
};

type Position = {
  x: number;
  y: number;
};

@Component({
  selector: 'app-domino',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './domino.component.html',
  styleUrls: ['./domino.component.css']
})
export class DominoComponent implements OnInit {

  dragging = false;

  startX = 0;
  startY = 0;

  selectedCells: Position[] = [];

  textBoxes: TextBox[] = [];

  cellSize = 40;

  zoom = 1;

  grid: Cell[][] = [];

  game?: GameResponse;
  loading = false;
  errorMessage = '';

  minX = -3;
  maxX = 3;
  minY = -3;
  maxY = 3;

  searchControl = new FormControl('');

  showSuggestions = false;
  filteredOptions: string[] = [];


  constructor(
    private gameService: GameService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() { 
    this.createGame();

    this.searchControl.valueChanges
    .pipe(
      distinctUntilChanged(),

      tap(value => {

        console.log("VALUE CHANGED:", value);
        const query = (value ?? '').trim();

        if (query.length === 0) {
          this.filteredOptions = [];
          this.showSuggestions = false;
        }
      }),

      filter(value => !!value && value.trim().length > 0),

      switchMap(value =>
        this.gameService.getSuggestions(value!.trim())
      )
    )
    .subscribe({
      next: suggestions => {
        this.filteredOptions = suggestions;
        this.showSuggestions = suggestions.length > 0;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error(err);
        this.filteredOptions = [];
      }
    });
  }

  gridTemplateColumns(): string {
    const cols = this.maxX - this.minX + 1;
    return `repeat(${cols}, ${this.cellSize}px)`;
  }

  createGame() {
    this.loading = true;
    this.errorMessage = '';

    const today = new Date().toISOString().split('T')[0];

    this.gameService.createGame(today).subscribe({
      next: game => {
        this.game = game;

        const obstacles = game.obstacles ?? [];
        const pieces = game.pieces ?? [];

        this.computeBounds(obstacles);
        this.buildGrid();
        this.applyObstacles(obstacles);
        this.applyPieces(pieces);

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('CREATE GAME ERROR:', err);
        this.errorMessage = this.getRequestErrorMessage(err);
        this.loading = false;
      }
    });
  }

  private computeBounds(obstacles: any[]) {
    if (obstacles.length === 0) {
      this.minX = this.maxX = this.minY = this.maxY = 0;
      return;
    }

    const xs = obstacles.map(o => o.x);
    const ys = obstacles.map(o => o.y);

    this.minX = Math.min(...xs);
    this.maxX = Math.max(...xs);
    this.minY = Math.min(...ys);
    this.maxY = Math.max(...ys);

  }

  private buildGrid() {
    const rows = this.maxY - this.minY + 1;
    const cols = this.maxX - this.minX + 1;

    this.grid = Array.from({ length: rows }, () =>
      Array.from({ length: cols }, () => ({
        image: 'domino/field.png',
        type: 'EMPTY' as CellType 
      }))
    );
  }

  private applyPieces(pieces: Piece[]) {
    this.textBoxes = [];

    pieces.forEach(o => {
      const x = o.pos.x - this.minX;
      const y = this.maxY - o.pos.y;

      const city = o.cityName;

      switch (o.direction) {

        case "UP": {
          this.setCell(x, y, 'domino/piece_down.png');
          this.setCell(x, y - 1, 'domino/piece_up.png');

          this.setCellPiece(x, y);
          this.setCellPiece(x, y - 1);

          this.addTextBox(x, y, city, 'UP');
          break;
        }

        case "DOWN": {
          this.setCell(x, y + 1, 'domino/piece_down.png');
          this.setCell(x, y, 'domino/piece_up.png');

          this.setCellPiece(x, y);
          this.setCellPiece(x, y + 1);

          this.addTextBox(x, y, city, 'DOWN');
          break;
        }

        case "RIGHT": {
          this.setCell(x, y, 'domino/piece_left.png');
          this.setCell(x + 1, y, 'domino/piece_right.png');

          this.setCellPiece(x, y);
          this.setCellPiece(x + 1, y);

          this.addTextBox(x, y, city, 'RIGHT');
          break;
        }

        case "LEFT": {
          this.setCell(x - 1, y, 'domino/piece_left.png');
          this.setCell(x, y, 'domino/piece_right.png');

          this.setCellPiece(x, y);
          this.setCellPiece(x - 1, y);

          this.addTextBox(x, y, city, 'LEFT');
          break;
        }
      }
    });
  }

  getTextBoxStyle(box: TextBox) {
    const size = this.cellSize;

    const style: any = {
      position: 'absolute',
      left: `${box.x * size}px`,
      top: `${box.y * size}px`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: `${size * 0.35}px`
    };

    switch (box.direction) {
      case 'RIGHT':
        style.width = `${size * 2}px`;
        style.height = `${size}px`;
        break;

      case 'LEFT':
        style.width = `${size * 2}px`;
        style.height = `${size}px`;
        style.transform = 'rotate(180deg)';
        break;

      case 'DOWN':
        style.width = `${size}px`;
        style.height = `${size * 2}px`;
        style.writingMode = 'vertical-rl';
        break;

      case 'UP':
        style.width = `${size}px`;
        style.height = `${size * 2}px`;
        style.writingMode = 'vertical-rl';
        style.transform = 'rotate(180deg)';
        break;
    }

    return style;
  }

  private addTextBox(
    x: number,
    y: number,
    text: string,
    direction: Direction
  ) {
    let boxX = x;
    let boxY = y;

    switch (direction) {
      case 'UP':
        // piece jest na dole, textbox obejmuje current + cell above
        boxY = y - 1;
        break;

      case 'DOWN':
        // current + below
        boxY = y;
        break;

      case 'RIGHT':
        // current + right
        boxX = x;
        break;

      case 'LEFT':
        // left + current
        boxX = x - 1;
        break;
    }

    this.textBoxes.push({
      x: boxX,
      y: boxY,
      text,
      direction
    });
  }

  private applyObstacles(obstacles: Obstacle[]) {
    obstacles.forEach(o => {
      const x = o.x - this.minX;
      const y = this.maxY - o.y;

      this.setCell(x, y, 'domino/obstacle.png');
      this.setCellObstacle(x, y);
    });
  }

  private setCell(x: number, y: number, image: string) {
    if (this.grid[y] && this.grid[y][x]) {
      this.grid[y][x].image = image;
    }
  }

  private setCellObstacle(x: number, y: number) {
    if (this.grid[y] && this.grid[y][x]) {
      this.grid[y][x].type = 'OBSTACLE' as CellType;
    }
  }

  private setCellPiece(x: number, y: number) {
    if (this.grid[y] && this.grid[y][x]) {
      this.grid[y][x].type = 'PIECE' as CellType;
    }
  }

  private key(x: number, y: number) {
    return `${x},${y}`;
  }

  startSelection(x: number, y: number) {
    if (this.loading || this.game?.complete) return;

    this.dragging = true;
    this.startX = x;
    this.startY = y;
    this.selectedCells = [];
    this.selectedCells.push({
        x: x,
        y: y
    });
  }

  updateSelection(x: number, y: number) {
    if (this.loading || this.game?.complete) return;
    if (!this.dragging) return;

    this.selectedCells = [];

    const dx = x - this.startX;
    const dy = y - this.startY;

    // wybieramy dominujący kierunek ruchu
    if (Math.abs(dx) >= Math.abs(dy)) {
      // poziomo 2x1
      this.selectedCells.push({
        x: this.startX,
        y: this.startY
      });

      if (dx > 0) {
        this.selectedCells.push({
          x: this.startX + 1,
          y: this.startY
        });
      } else {
        this.selectedCells.push({
          x: this.startX - 1,
          y: this.startY
        });
      }
    } else {
      // pionowo 1x2
      this.selectedCells.push({
        x: this.startX,
        y: this.startY
      });

      if (dy > 0) {
        this.selectedCells.push({
          x: this.startX,
          y: this.startY + 1
        });
      } else {
        this.selectedCells.push({
          x: this.startX,
          y: this.startY - 1
        });
      }
    }
  }

  endSelection() {
    if (this.loading || this.game?.complete) {
      this.dragging = false;
      this.selectedCells = [];
      return;
    }

    this.dragging = false;

    if(this.selectedCells.length<2)
    {
      this.selectedCells = [];
      return;
    }

    const overlaps = this.selectedCells.some(cell =>
      this.grid[cell.y][cell.x].type !== 'EMPTY'
    );

    if (overlaps) {
      this.selectedCells = [];
      return;
    }

    console.log([...this.selectedCells]);
  }

  isSelected(x: number, y: number): boolean {
    return this.selectedCells.some(c => c.x === x && c.y === y);
  }

  select(option: string) {
    if (this.loading || this.game?.complete) return;

    this.searchControl.setValue(option);
    this.filteredOptions = [];
    this.showSuggestions = false;
  }

  hideSuggestions() {
    setTimeout(() => {
      this.showSuggestions = false;
    }, 150);
  }

  addPiece() {
    if (this.loading || this.game?.complete) return;
    this.errorMessage = '';

    if(this.selectedCells.length!=2)
    {
      //error
      return;
    }
    const cell_0 = this.selectedCells[0];
    const cell_1 = this.selectedCells[1];
    let direction = "";
    if(cell_1.x == cell_0.x+1 && 
      cell_1.y == cell_0.y
    )
    {
      direction = "RIGHT";
    }
    else if(cell_1.x == cell_0.x-1 && 
      cell_1.y == cell_0.y
    )
    {
      direction = "LEFT";
    }
    else if(cell_1.x == cell_0.x && 
      cell_1.y == cell_0.y - 1
    )
    {
      direction = "UP";
    }
    else if(cell_1.x == cell_0.x && 
      cell_1.y == cell_0.y + 1
    )
    {
      direction = "DOWN";
    }
    const standardPos = {
      x: this.minX + cell_0.x,
      y: this.maxY - cell_0.y
    };
    const city = this.searchControl.value?.trim() ?? '';
    if(city.length == 0)
    {
      //error
      return;
    }
    this.loading = true;
    this.gameService.move(city, standardPos, this.game?.gameId ?? -1, direction).subscribe({
      next: game => {
        this.game = game;
        this.searchControl.setValue('');
        this.filteredOptions = [];
        this.showSuggestions = false;
        if (game.complete) {
          this.dragging = false;
          this.selectedCells = [];
          this.filteredOptions = [];
          this.showSuggestions = false;
        }

        const obstacles = game.obstacles ?? [];
        const pieces = game.pieces ?? [];

        this.computeBounds(obstacles);
        this.buildGrid();
        this.applyObstacles(obstacles);
        this.applyPieces(pieces);

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('MOVE ERROR:', err);
        this.errorMessage = this.getRequestErrorMessage(err);
        this.loading = false;
      }
    });
  }

  private getRequestErrorMessage(error: any): string {
    return error?.error?.detail
      ?? error?.error?.message
      ?? error?.message
      ?? 'Nie udało się połączyć z grą. Spróbuj ponownie.';
  }
}