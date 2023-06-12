import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {SignalListPageComponent} from "./signal-list-page/signal-list-page.component";
import {CreateFeedComponent} from "./create-signal/create-feed.component";
import {FeedViewComponent} from "./feed-view/feed-view.component";

const routes: Routes = [
  {path: '', component: SignalListPageComponent},
  {path: 'signals/new', component: CreateFeedComponent},
  {path: 'feeds/:id', component: FeedViewComponent},

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
