import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FeedViewComponent } from './feed-view.component';
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {TuiButtonModule, TuiScrollbarModule, TuiSvgModule} from "@taiga-ui/core";
import {TuiTableModule} from "@taiga-ui/addon-table";
import {IntersectionObserverModule} from "@ng-web-apis/intersection-observer";
import {TuiAccordionModule} from "@taiga-ui/kit";



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
        IntersectionObserverModule,
        TuiAccordionModule,
        TuiSvgModule
    ]
})
export class FeedViewModule { }
