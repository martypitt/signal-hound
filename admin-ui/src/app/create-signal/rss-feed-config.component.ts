import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, ValidationErrors, Validators} from "@angular/forms";

@Component({
  selector: 'app-rss-feed-config',
  template: `
      <form [formGroup]="formGroup">
          <div class="row">
              <tui-input
                      formControlName="url"
                      [tuiTextfieldCleaner]="true"
              >
                  Feed URL
                  <span class="tui-required"></span>
                  <input
                          tuiTextfield
                          placeholder="Feed URL"
                  />
              </tui-input>
            <tui-error
              formControlName="url"
              [error]="[] | tuiFieldError | async"
            ></tui-error>
          </div>
          <div class="row">
              <div
                      tuiGroup
                      class="group"
              >
                  <div>
                      <tui-input-number
                              formControlName="pollFrequency"
                      >
                          Poll frequency
                          <input
                                  tuiTextfield
                                  placeholder="60"
                          />
                      </tui-input-number>
                      <tui-error
                              formControlName="pollFrequency"
                              [error]="[] | tuiFieldError | async"
                      ></tui-error>
                  </div>
                  <div>
                      <tui-select formControlName="pollPeriod"
                      >
                          Duration type
                          <input
                                  tuiTextfield
                                  placeholder="Period"
                          />
                          <tui-data-list-wrapper
                                  *tuiDataList
                                  [items]="durationTypes"
                          ></tui-data-list-wrapper>
                      </tui-select>
                      <tui-error
                              formControlName="pollPeriod"
                              [error]="[] | tuiFieldError | async"
                      ></tui-error>
                  </div>


              </div>

          </div>

      </form>
  `,
  styleUrls: ['./rss-feed-config.component.scss']
})
export class RssFeedConfigComponent {

  urlPattern = '(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?' as const;

  formGroup = new FormGroup({
    url: new FormControl('',[Validators.required, Validators.pattern(this.urlPattern)]),
    pollFrequency: new FormControl<number|null>(null, Validators.required),
    pollPeriod: new FormControl<string|null>(null, Validators.required)
  })
  durationTypes: Period[] = ['Seconds', 'Minutes' , 'Hours'];

  constructor() {


    this.formGroup.valueChanges
      .subscribe(updates => {
        const updatedConfig = updates as RssConfig;
        this.rssConfigChange.emit(updatedConfig);
        this.validChange.emit(this.formGroup.valid);
        this.errorsChange.emit(this.formGroup.errors);
      })
  }

  @Input()
  rssConfig: RssConfig;

  @Output()
  rssConfigChange = new EventEmitter<RssConfig>();

  @Output()
  validChange = new EventEmitter<boolean>;

  @Output()
  errorsChange = new EventEmitter<ValidationErrors | null>();

  get errors(): ValidationErrors | null  {
    return this.formGroup.errors;
  }
  get valid(): boolean {
    return this.formGroup.valid;
  }

}

export interface RssConfig {
  url: string;
  pollFrequency: number;
  pollPeriod: Period;

}

export type Period = 'Seconds' | 'Minutes' | 'Hours' ;
