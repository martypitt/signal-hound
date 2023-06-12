import {NgDompurifySanitizer} from "@tinkoff/ng-dompurify";
import {TuiRootModule, TuiDialogModule, TuiAlertModule, TUI_SANITIZER} from "@taiga-ui/core";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {SignalListPageModule} from "./signal-list-page/signal-list-page.module";
import {SidebarModule} from "./sidebar/sidebar.module";
import {NavbarModule} from "./navbar/navbar.module";
import {CreateFeedComponent} from "./create-signal/create-feed.component";
import {CreateSignalModule} from "./create-signal/create-signal.module";
import {HttpClientModule} from "@angular/common/http";
import {FeedViewModule} from "./feed-view/feed-view.module";

@NgModule({
  declarations: [
    AppComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    SignalListPageModule,
    CreateSignalModule,
    SidebarModule,
    NavbarModule,
    BrowserAnimationsModule,
    TuiRootModule,
    TuiDialogModule,
    TuiAlertModule,
    HttpClientModule,
    FeedViewModule
  ],
  providers: [{provide: TUI_SANITIZER, useClass: NgDompurifySanitizer}],
  bootstrap: [AppComponent]
})
export class AppModule {
}
