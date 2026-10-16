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
      title: 'Wkrótce',
      description: 'Kolejna gra już niedługo dołączy do kolekcji.',
      icon: '✳',
      route: '/page2'
    }
  ];

}