import {NgModule} from '@angular/core';
import {SignalListPageComponent} from "./signal-list-page.component";
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
  exports: [SignalListPageComponent],
  declarations: [SignalListPageComponent],
  providers: [],
})
export class SignalListPageModule {
}
