# super-rss

Library to get a RSS feed, even if the target website does not offer one.
If there is a proper RSS feed it will use the excellent [remus](https://github.com/igrishaev/remus), otherwise, it will try to create a feed using another HTTP resource.

## Rationale
Websites don't necessarily offer an RSS feed but most of them offer a sitemap to improve their SEO ranking.
This library attempts to create a feed using alternative methods such as using a sitemap or parsing a web page to extract the links.

## Usage

Try multiple methods to create an RSS feed, from the more natural to the most "hacky"
```clj
(require '[super-rss :as sr]])

(sr/get-feed "http://website.com/" {} {})

; Return
{:method [:find-rss-url "http://website.com/posts.atom"],
 :result [{:description "",
           :updated-date #inst "2020-10-14T23:58:08.000-00:00",
           :published-date #inst "2020-10-14T21:43:45.000-00:00",
           :title "Bla bla",
           :link "http://website.com/posts/blabla"}]}
```
Try to create a feed from the sitemap
```clj
(sr/get-feed "http://website.com/" {:method :sitemap} {})
```

Try to create a feed from the links on "http://website.com/posts"
```clj
(sr/get-feed "http://website.com/posts" {:method :page-links} {})
```

Avoid crawling a page already ingested by passing an extra function.
Useful to not crawl over and over the same page for big sitemap.
```clj
(defn already-ingest? [url]
  ; your db call to check if the URL needs to be crawled or if you already have the result in your database
  ...
  )
(sr/get-feed "http://website.com/" {:method :sitemap} {:already-ingest? already-ingest?})
```

## Telling failures apart
Every exception super-rss raises carries `ex-data`, so a caller can tell a permanently
dead source from a blip instead of counting all failures the same:
```clj
{:super-rss/error :dns     ; :dns :timeout :http-status :challenge :parse :unknown
 :url    "https://example.com/feed"
 :status 403               ; only when the failure arrived with an HTTP response
 :cause  "example.com"}
```
`:dns` and a `404` are worth giving up on; `:timeout` and `:challenge` are worth retrying.
Note that `:challenge` keys on `cf-mitigated` or the interstitial body, never on
`server: cloudflare` - plenty of ordinary origin errors are served through Cloudflare.

This describes what is *raised*. Pass `{:throw? true}` to get exceptions at all: by default
a failing strategy is swallowed and `get-feed` returns `nil`, which is also what a strategy
that simply found nothing returns. A challenge is only ever recognised where the response
headers are in hand - `remus` reports a non-2xx with the status alone, so a feed fetched
directly behind a bot block classifies `:http-status` rather than `:challenge`.

A strategy that throws does not end the run: it is skipped and the next one gets its turn,
so a broken sitemap no longer hides a page the link scraper reads fine. `get-feed` only
raises when *every* strategy failed and at least one threw. The exception then carries the
most informative failure - the first one that is not `:unknown`, since the strategies run
from the most specific to the most hacky - plus every failure in run order:
```clj
{:super-rss/error :dns
 :url    "https://example.com"
 :cause  "example.com"
 :method :find-rss-url                 ; the strategy whose failure speaks for the run
 :errors [{:method :find-rss-url :super-rss/error :dns     :url "https://example.com" :cause "example.com"}
          {:method :sitemap      :super-rss/error :unknown :url "https://example.com" :cause "..."}]}
```

## Limitations
- Filtering what looks like a feed entry won't work all the time
- Parsing an HTML page for finding a date is obviously not gonna work all the time.
- Only XML sitemap are supported

## License

Copyright © 2021 Julien Bille

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
