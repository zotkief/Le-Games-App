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
      title: "Podstrona 1",
      route: "/domino"
    },
    {
      title: "Podstrona 2",
      route: "/page2"
    }
  ];

}