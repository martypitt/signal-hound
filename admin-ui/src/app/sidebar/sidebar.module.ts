import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from './sidebar.component';
import {RouterLinkWithHref} from "@angular/router";
import {TuiSvgModule} from "@taiga-ui/core";



@NgModule({
  declarations: [
    SidebarComponent
  ],
  exports: [
    SidebarComponent
  ],
  imports: [
    CommonModule,
    RouterLinkWithHref,
    TuiSvgModule
  ]
})
export class SidebarModule { }
