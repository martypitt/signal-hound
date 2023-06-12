import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FeedViewComponent } from './feed-view.component';
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {TuiButtonModule, TuiScrollbarModule} from "@taiga-ui/core";
import {TuiTableModule} from "@taiga-ui/addon-table";
import {IntersectionObserverModule} from "@ng-web-apis/intersection-observer";



@NgModule({
  declarations: [
    FeedViewComponent
  ],
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    TuiButtonModule,
    TuiTableModule,
    TuiScrollbarModule,
    IntersectionObserverModule
  ]
})
export class FeedViewModule { }
