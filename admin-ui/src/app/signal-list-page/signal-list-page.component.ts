import {Component, OnInit} from '@angular/core';
import {FeedsService} from "../feeds.service";
import {FeedSpec} from "../create-signal/create-feed.component";
import {Observable} from "rxjs";
import {Router} from "@angular/router";

@Component({
  selector: 'app-signal-list-page',
  styleUrls: ['./signal-list-page.component.scss'],
  template: `
      <app-header-component-layout title="Signals">
          <ng-container ngProjectAs="buttons">
              <button tuiButton appearance="outline" size="m"
                      [routerLink]="['signals/new']"
              >Create new signal
              </button>
          </ng-container>

          <div>
              <ul>
                  <li *ngFor="let feed of feeds$ | async"
                      class="w-full mb-4 border rounded-lg border-slate-300 p-4 hover:border-slate-400 hover:cursor-pointer">
                      <a [routerLink]="['feeds',feed.id]">
                          <h4 class="text-lg">{{ feed.title }}</h4>
                      </a>
                  </li>
              </ul>


          </div>
      </app-header-component-layout>
  `
})
export class SignalListPageComponent {
  feeds$: Observable<FeedSpec[]>;

  constructor(private feedsService: FeedsService, private router: Router) {
    this.feeds$ = feedsService.listFeeds();
  }


}
