import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CreateFeedComponent } from './create-feed.component';
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {
  TuiDataListWrapperModule, TuiFieldErrorPipeModule,
  TuiInputModule, TuiInputNumberModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule,
  TuiTextAreaModule
} from "@taiga-ui/kit";
import {
  TuiButtonModule, TuiErrorModule,
  TuiGroupModule, TuiHintModule, TuiNotificationModule,
  TuiScrollbarModule,
  TuiSvgModule,
  TuiTextfieldControllerModule
} from "@taiga-ui/core";
import { RssFeedConfigComponent } from './rss-feed-config.component';
import { FieldTableComponent } from './field-table.component';
import {IntersectionObserverModule} from "@ng-web-apis/intersection-observer";
import {TuiTableModule} from "@taiga-ui/addon-table";
import {TuiValidatorModule} from "@taiga-ui/cdk";



@NgModule({
  declarations: [
    CreateFeedComponent,
    RssFeedConfigComponent,
    FieldTableComponent
  ],
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    ReactiveFormsModule,
    TuiInputModule,
    TuiTextfieldControllerModule,
    TuiSelectModule,
    TuiDataListWrapperModule,
    TuiStringifyContentPipeModule,
    TuiGroupModule,
    IntersectionObserverModule,
    TuiScrollbarModule,
    TuiTableModule,
    FormsModule,
    TuiTextAreaModule,
    TuiSvgModule,
    TuiButtonModule,
    TuiValidatorModule,
    TuiHintModule,
    TuiFieldErrorPipeModule,
    TuiErrorModule,
    TuiInputNumberModule,
    TuiNotificationModule
  ]
})
export class CreateSignalModule { }
