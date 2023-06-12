import {NgModule} from '@angular/core';
import {FeedListPageComponent} from "./feed-list-page.component";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {TuiButtonModule} from "@taiga-ui/core";
import {RouterLink, RouterLinkWithHref} from "@angular/router";
import {AsyncPipe, NgForOf} from "@angular/common";

@NgModule({
  imports: [
    HeaderComponentLayoutModule,
    TuiButtonModule,
    RouterLink,
    NgForOf,
    AsyncPipe,
    RouterLinkWithHref
  ],
  exports: [FeedListPageComponent],
  declarations: [FeedListPageComponent],
  providers: [],
})
export class FeedListPageModule {
}
