import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {FeedsService, SignalParseResult} from "../feeds.service";
import {mergeMap, Observable} from "rxjs";
import {FeedSpec} from "../create-signal/create-feed.component";
import {RssConfig} from "../create-signal/rss-feed-config.component";
import {environment} from "../../environments/environment";
import {TuiAlertService} from "@taiga-ui/core";

@Component({
  selector: 'app-feed-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <app-header-component-layout [title]="feedSpec?.title!" [wide]="true">
          <ng-container ngProjectAs="buttons">
              <button tuiButton appearance="outline" size="m" (click)="executeFeed()" [showLoader]="executing">Run now
              </button>
          </ng-container>
          <ng-container ngProjectAs="header-components">
              <span>Rss feed from <a [href]="rssConfig?.url" target="_blank">{{rssConfig?.url}}</a></span>
              <div class="mt-8">
                  <tui-accordion-item class="container">
                      <div class="text-sm">API available at <span
                              class="font-mono">{{hydrateUrl(feedSpec?.apiEndpoint)}}</span></div>
                      <div class="text-xs">Expand for more API details</div>
                      <ng-template tuiAccordionItemContent>
                          <table>
                              <tr *ngFor="let endpoint of apiEndpoints">
                                  <td class="text-sm pr-8">{{endpoint.label}}</td>
                                  <td class="text-sm">
                                      <div class="flex items-center">
                                        {{ endpoint.value }}
                                        <tui-svg class="ml-4 cursor-pointer text-gray-400 group-hover:text-indigo-600" src="tuiIconCopy" (click)="copyToClipboard(endpoint.value)"></tui-svg>
                                      </div>
                                  </td>
                              </tr>
                          </table>
                      </ng-template>
                  </tui-accordion-item>
              </div>
          </ng-container>

          <!--        <tui-scrollbar-->
          <!--          waIntersectionRoot-->
          <!--          class="scrollbar"-->
          <!--          [hidden]="true"-->
          <!--        >-->
          <div class="overflow-scroll">
              <table tuiTable class="table" style="border-collapse: collapse"
                     *ngIf="feedSpec" [columns]="columns">
                  <thead>
                  <tr tuiThGroup>
                      <th tuiTh>Signal type</th>
                      <th tuiTh>Signal URI</th>
                      <th tuiTh>Result</th>
                      <th tuiTh>Error message</th>
                      <th tuiTh *ngFor="let col of feedSpec.extractions" [resizable]="true">{{ col.title }}</th>
                  </tr>
                  </thead>
                  <tbody tuiTbody [data]="feedData">
                  <tr *ngFor="let row of feedData" tuiTr>
                      <td *tuiCell="'signalType'">{{row.source.id.feedType}}</td>
                      <td *tuiCell="'uri'"><a [attr.href]="row.source.signalUri"
                                              target="_blank">{{row.source.signalUri}}</a></td>
                      <td *tuiCell="'success'">{{ row.success}}</td>
                      <td *tuiCell="'errorMessage'">{{ row.errorMessage}}</td>
                      <ng-container *ngFor="let col of feedSpec.extractions">
                          <td *tuiCell="col.title">
                              <ng-container *ngIf="row.success">
                                  {{ row.signals[col.title]}}
                              </ng-container>
                          </td>
                      </ng-container>

                  </tr>

                  </tbody>
              </table>
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

  get rssConfig():RssConfig | null {
    if (this.feedSpec?.type !== "RssFeed") {
      return null;
    }
    return this.feedSpec?.config as RssConfig
  }

  get apiEndpoints():{ label: string, value: string }[] {
    if (this.feedSpec === null) {
      return [];
    }
    return [
      {label: 'API Endpoint', value: this.hydrateUrl(this.feedSpec?.apiEndpoint!)!},
      {label: 'OpenAPI Spec', value: this.hydrateUrl(this.feedSpec?.openApiSpecPath!)!},
      {label: 'Regenerate OpenAPI Spec', value: this.hydrateUrl(this.feedSpec?.regenerateApiSpecPath!)!}
    ]
  }

  constructor(private activeRoute: ActivatedRoute,
              private feedService: FeedsService,
              private changeDetectorRef: ChangeDetectorRef,
              private alertService: TuiAlertService
              ) {
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
        this.columns = ['signalType', 'uri', 'success', 'errorMessage'].concat(this.feedSpec.extractions!.map(v => v.title))
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
        error: err => {
          this.executing = false;
          this.changeDetectorRef.detectChanges();
        }
      })
  }

  hydrateUrl(apiEndpoint: string | null | undefined): string | null {
    if (apiEndpoint === null || apiEndpoint === undefined) {
      return null;
    }
    return `${environment.serverUrl}${apiEndpoint}`
  }
  async copyToClipboard(text: string | null) {
    if (text === null) {
      return
    }
    try {
      await navigator.clipboard.writeText(text)
      this.alertService.open('Copied to clipboard').subscribe()
      this.changeDetectorRef.markForCheck();
      console.log('Text copied to clipboard');

    } catch (err) {
      this.alertService.open('Something went wrong copying to the clipboard').subscribe()
      console.log('Failed to copy text: ', err);
    }
  }
}

