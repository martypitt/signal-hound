import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {FeedsService, SignalParseResult} from "../feeds.service";
import {mergeMap, Observable} from "rxjs";
import {FeedSpec} from "../create-signal/create-feed.component";

@Component({
  selector: 'app-feed-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-header-component-layout [title]="feedSpec?.title!">
      <ng-container ngProjectAs="buttons">
        <button tuiButton appearance="outline" size="m" (click)="executeFeed()" [showLoader]="executing">Run now</button>
      </ng-container>

      <div>
        <tui-scrollbar
          waIntersectionRoot
          class="scrollbar"
          [hidden]="true"
        >
          <table tuiTable class="table" style="border-collapse: collapse" *ngIf="feedSpec" [columns]="columns">
            <thead>
            <tr tuiThGroup>
              <th tuiTh>Signal type</th>
              <th tuiTh>Signal URI</th>
              <th tuiTh *ngFor="let col of feedSpec.extractions" [resizable]="true">{{ col.title }}</th>
            </tr>
            </thead>
            <tbody tuiTbody [data]="feedData">
            <tr *ngFor="let row of feedData" tuiTr>
              <td *tuiCell="'signalType'">{{row.source.id.feedType}}</td>
              <td *tuiCell="'uri'"><a [attr.href]="row.source.signalUri"
                                      target="_blank">{{row.source.signalUri}}</a></td>
              <ng-container *ngFor="let col of feedSpec.extractions">
                <td *tuiCell="col.title">{{ row.signals[col.title]}}</td>
              </ng-container>

            </tr>

            </tbody>
          </table>
        </tui-scrollbar>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./feed-view.component.scss']
})
export class FeedViewComponent {

  feedSpec: FeedSpec | null = null;
  feed: Observable<FeedSpec>
  feedId: string;
  executing: boolean = false;

  feedData: SignalParseResult[] = [];
  columns: string[] = [];

  constructor(private activeRoute: ActivatedRoute, private feedService: FeedsService, private changeDetectorRef: ChangeDetectorRef) {
    activeRoute.paramMap
      .pipe(mergeMap(params => {
        const id = params.get('id');
        if (id === null) {
          throw 'Expected to receive :id'
        }
        this.feedId = id;
        return feedService.getFeed(id)
      })).subscribe({
      next: value => {
        this.feedSpec = value;
        this.columns = ['signalType', 'uri'].concat(this.feedSpec.extractions!.map(v => v.title))
        this.changeDetectorRef.markForCheck();
      }
    });
  }


  executeFeed() {
    this.feedData = [];
    this.executing = true;
    this.feedService.executeFeed(this.feedId)
      .subscribe({
        next: value => {
          const newFeedData = [...this.feedData, value]
          this.feedData = newFeedData;
          this.changeDetectorRef.detectChanges();
        },
        error: err => this.executing = false
      })
  }
}
