import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    RouterLink
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

  tiles = [
    {
      title: 'Domino',
      description: 'Układaj elementy i odkrywaj nowe miasta.',
      icon: '◈',
      route: '/domino'
    },
    {
      title: 'Zgadnij miasto',
      description: 'Rozpoznaj miasto po przebiegu jego tras tramwajowych.',
      icon: '✳',
      route: '/guess-city'
    }
  ];

}