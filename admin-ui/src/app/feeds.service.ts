import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {FeedSpec} from "./create-signal/create-feed.component";
import {Observable} from "rxjs";
import {environment} from "../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class FeedsService {

  constructor(private httpClient: HttpClient) {
  }

  executeFeed(id: string, limit: number | null = null): Observable<SignalParseResult> {
    return new Observable(observer => {
      const limitParam = limit ? `?limit=${limit}` : "";
      const eventSource = this.getEventSource(`${environment.serverUrl}/feeds/${id}/stream${limitParam}`);

      eventSource.onmessage = event => {
        if (event.type === 'EndOfStream') {
          observer.complete()
          eventSource.close();
        } else {
          observer.next(JSON.parse(event.data) as SignalParseResult);
        }
      };

      eventSource.onerror = error => {
        // We get an error event when the stream closes on the server.
        observer.error(error);
        observer.complete()
        eventSource.close();
      }
    });

  }

  getFeed(id: string): Observable<FeedSpec> {
    return this.httpClient.get<FeedSpec>(`${environment.serverUrl}/feeds/${id}`)
  }

  saveFeed(feedSpec: FeedSpec): Observable<FeedSpec> {
    return this.httpClient.put<FeedSpec>(`${environment.serverUrl}/feeds`, feedSpec)
  }

  listFeeds(): Observable<FeedSpec[]> {
    return this.httpClient.get<FeedSpec[]>(`${environment.serverUrl}/feeds`)
  }

  private getEventSource(url: string): EventSource {
    return new EventSource(url);
  }
}

export interface SignalParseResult {
  source: SignalSource;
  success: boolean;
  signalUri: string;
  errorMessage: string | null;
  signals: { [index: string]: any }
}

export interface SignalSource {
  id: {
    feedType: string;
    id: string;
  }
  signalUri: string;
}
