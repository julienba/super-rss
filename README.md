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
