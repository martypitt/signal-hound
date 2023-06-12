import {Component, Inject} from '@angular/core';
import {FormControl, FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {Extraction} from "./field-table.component";
import {FeedsService} from "../feeds.service";
import {TuiAlertService} from "@taiga-ui/core";
import {Router} from "@angular/router";

@Component({
  selector: 'app-create-signal',
  template: `
    <app-header-component-layout title="Create new feed">
      <div>
        <form [formGroup]="formGroup">
          <div class="row">
            <tui-input
              formControlName="title"
              (blur)="suggestApiEndpoint()"
              [tuiTextfieldCleaner]="true"
            >
              Title
              <span class="tui-required"></span>
              <input
                tuiTextfield
                (blur)="suggestApiEndpoint()"
                placeholder="Title"
              />
            </tui-input>
            <tui-error
              formControlName="title"
              [error]="[] | tuiFieldError | async"
            ></tui-error>
          </div>
          <div class="row">
            <tui-select formControlName="type"
                        [stringify]="stringifyFeedType"
            >
              Feed type
              <input
                tuiTextfield
                placeholder="Select a feed type"
              />
              <tui-data-list-wrapper
                *tuiDataList
                [items]="feedTypes"
                [itemContent]="stringifyFeedType | tuiStringifyContent"
              ></tui-data-list-wrapper>
            </tui-select>
          </div>
          <ng-container [ngSwitch]="feedType">
            <app-rss-feed-config *ngSwitchCase="'RssFeed'" (rssConfigChange)="updateFeedSpec('config', $event)"
                                 (errorsChange)="configErrors = $event" (validChange)="configValid = $event"
            >

            </app-rss-feed-config>
            <div *ngSwitchDefault
                 class="w-full flex p-8 mb-4 text-lg font-light border border-dashed border-slate-400 rounded-lg place-content-center">
              <div>Select a feed type, and configuration options will appear here</div>
            </div>
          </ng-container>
          <div class="row">
            <tui-input-number formControlName="cacheDurationSeconds"
                              tuiTextfieldPostfix=" seconds">Cache duration (in seconds)

            </tui-input-number>
            <tui-error
              formControlName="cacheDurationSeconds"
              [error]="[] | tuiFieldError | async"
            ></tui-error>
          </div>
          <div class="row">
            <tui-input formControlName="apiEndpoint">API Endpoint
            </tui-input>
            <div class="text-sm mt-2 text-slate-500">Start with a slash. Letters, numbers and hyphens only.</div>
            <tui-error
              formControlName="apiEndpoint"
              [error]="[] | tuiFieldError | async"
            ></tui-error>
          </div>
        </form>
        <app-field-table (extractionsChange)="updateFeedSpec('extractions', $event)"></app-field-table>
      </div>

      <div class="row mt-8" *ngIf="errorMessage">
        <tui-notification status="error">{{errorMessage}}</tui-notification>
      </div>


      <div class="my-8">

<!--        <button tuiButton appearance="outline" size="m" class="mr-8">Test it out</button>-->
        <button tuiButton appearance="primary" size="m" [disabled]="!isValid || working" [showLoader]="working"
                (click)="saveFeed()">Save
        </button>
      </div>


    </app-header-component-layout>
  `,
  styleUrls: ['./create-feed.component.scss']
})
export class CreateFeedComponent {


  readonly stringifyFeedType = (item: FeedType) => {
    switch (item) {
      case "RssFeed":
        return 'Rss Feed';
      default:
        return item;
    }
  }
  configErrors: ValidationErrors | null = null;
  configValid: boolean = false;

  errorMessage: string | null = null;

  working = false;

  feedSpec: FeedSpec = {
    id: null,
    title: null,
    config: null,
    extractions: null,
    type: null,
    apiEndpoint: null,
    cacheDurationSeconds: 3600
  }

  feedTypes: FeedType[] = ['RssFeed']

  formGroup: FormGroup

  get isValid() {
    return this.formGroup.valid && this.configValid && (this.feedSpec.extractions?.length || 0) > 0
  }

  updateFeedSpec(key: keyof FeedSpec, value: any) {
    this.feedSpec[key] = value;
    this.logUpdate();

  }

  get feedType(): string | null {
    return this.formGroup.getRawValue().type;
  }

  constructor(private service: FeedsService,
              @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
              private router: Router
  ) {
    this.formGroup = new FormGroup<any>({
      title: new FormControl(null, Validators.required),
      type: new FormControl(null, Validators.required),
      cacheDurationSeconds: new FormControl(this.feedSpec.cacheDurationSeconds, [Validators.required, Validators.min(0)]),
      apiEndpoint: new FormControl(null, Validators.pattern('^\\/[a-zA-Z\\-]+\$'))
    })
    this.formGroup.valueChanges.subscribe(update => {
      const updateValue = update as Partial<FeedSpec>
      Object.keys(updateValue).forEach(key => {
        const tKey = key as keyof FeedSpec;
        this.feedSpec[tKey] = updateValue[tKey]
      });
      this.logUpdate();
    })
  }

  private logUpdate() {
    console.log('Updated', this.feedSpec);
  }

  saveFeed() {
    this.working = true;
    this.service.saveFeed(this.feedSpec).subscribe({
      next: result => {
        this.alerts.open(`Feed ${result.title} was created successfully`, {
          label: 'Feed created'
        }).subscribe();
        this.router.navigate([''])

      },
      error: err => {
        console.log(err)
        this.errorMessage = err.message;
        this.working = false;
      }
    })
  }

  suggestApiEndpoint() {

    if (!this.feedSpec.title) return;
    // Lowercase the string
    let suggestedApiPath = this.feedSpec.title!.toLowerCase();

    // Replace spaces with dashes
    suggestedApiPath = suggestedApiPath.replace(/\s+/g, '-');

    // Strip out illegal characters
    suggestedApiPath = suggestedApiPath.replace(/[^a-z\-]/g, '');

    // Prepend a leading slash
    suggestedApiPath = '/' + suggestedApiPath;

    this.feedSpec.apiEndpoint = suggestedApiPath;
    this.formGroup.get('apiEndpoint')?.setValue(suggestedApiPath);

  }
}

export interface FeedSpec {
  id: string | null;
  title: string | null;
  type: FeedType | null;
  config: any | null;
  extractions: Extraction[] | null;
  cacheDurationSeconds: number | null;
  apiEndpoint: string | null;

  // Server-generated components
  apiPath?: string;
  openApiSpecPath?: string;
  regenerateApiSpecPath?: string;
}

export type FeedType = 'RssFeed';
