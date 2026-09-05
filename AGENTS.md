# AGENTS.md

super-rss is a Clojure library that generates an RSS feed for any website, including ones that publish no native feed. Tests run on kaocha, linting on clj-kondo, tasks on babashka.

## Project map

- `src/super_rss/core.clj` — public entry point `get-feed`, and the `fetch` multimethod dispatching on strategy
- `src/super_rss/impl/normal.clj` — native RSS/Atom discovery and parsing via `remus`
- `src/super_rss/impl/sitemap.clj` — XML sitemap discovery (robots.txt or `<link>` tags) and parsing
- `src/super_rss/impl/smart_links.clj` — scrapes article links out of HTML using hickory zipper traversal
- `src/super_rss/impl/flat_smart_links.clj` — simplified, non-nested variant of smart-links
- `src/super_rss/impl/common.clj` — shared URL filtering and article-detection patterns
- `src/super_rss/html.clj` — HTML fetching and metadata extraction (title, description, dates)
- `src/super_rss/http.clj` — HTTP client wrapper around ok-http
- `src/super_rss/date.clj` — date parsing from assorted string formats
- `src/super_rss/robots_txt.clj` — robots.txt parsing to locate sitemaps
- `src/super_rss/hickory_zipper.clj` — hickory → zipper conversion
- `test/resources/` — HTML and XML fixtures for the smart-links and normal strategies

## Strategies

`get-feed` tries `fetch` methods in order until one returns entries, unless a single `:method` is passed in the opts:

1. `:direct-rss` — treat the URL as a feed URL and fetch it directly (not in the default order)
2. `:find-rss-url` — find a feed link in the page's `<link>` or anchor tags, then fetch it
3. `:sitemap` — build a feed from recent article URLs in an XML sitemap
4. `:smart-links` — extract article links, titles, descriptions and dates from HTML
5. `:flat-smart-links` — flattened variant of the above

<important if="you need to run commands to build, test, lint, or evaluate code">

| Command | What it does |
|---|---|
| `bb deps` | Prefetch all deps (default, `:dev`, `:test`, `:outdated`) |
| `bb test` | Run tests (wraps `clojure -M:test:test-runner`) |
| `bb lint` | Lint `src` and `test` with clj-kondo |
| `bb lint-init` | Lint the classpath and copy dependency configs — run once on setup |
| `bb outdated-check` | Report outdated deps |
| `bb outdated` | Upgrade outdated deps in place |
| `bb check` | lint + outdated-check + test |
| `clojure -M:test:test-runner` | Run tests directly; cloverage coverage is on by default (see `tests.edn`) |
| `clojure -M:test:test-runner --focus super-rss.date-test` | Run a single test namespace |
| `clj-kondo --lint <path>` | Lint one file or directory (also accepts `src test` or `.`) |
| `clj-nrepl-eval --discover-ports` | List running nREPL servers |
| `clj-nrepl-eval -p <port> "<code>"` | Evaluate Clojure in a live REPL; add `--timeout <ms>` for slow forms |
</important>

<important if="you are evaluating code in the REPL">
The nREPL session persists across evaluations, so namespaces and state are retained. Require namespaces with `:reload` or your edits will not be picked up.
</important>

<important if="you are working on smart-links or flat-smart-links extraction">

The algorithm in `impl/smart_links.clj` finds article-like links, then walks up the DOM 1–5 levels from the first link looking for a parent whose siblings hold multiple article entries. Titles come from heading tags, anchor text, or the URL path; dates and descriptions are searched for in nearby text nodes.

It requires more than `min-node` (2) matching siblings to accept a level as a list pattern, so single-entry pages will not match. Date extraction is best-effort and silently yields nothing on unusual formats.
</important>

<important if="you are changing which URLs are treated as articles">
`impl/common.clj` holds both regex patterns (`article-prefix`, `ignore-href-pattern`) and predicates (`blog-url?`). It includes paths like `/blog/`, `/post/`, `/news/`, `/article/`, `/insights/`, `/library/`, and excludes category, author, tag, pagination and social links. Change the heuristics there rather than in a single strategy.
</important>

<important if="you are working on sitemap parsing">
Only XML sitemaps are supported — other sitemap index formats are not. `page-crawl-limit` in `impl/sitemap.clj` caps the crawl at 3 pages.
</important>

<important if="you are debugging a feed that a server serves with the wrong content type">
`impl/normal.clj` deliberately ignores the content-type header and validates the body instead, checking for `<rss`, `<feed` or `<RDF`. Some servers return RSS as `text/html`.
</important>

<important if="you are adding a new fetch strategy">
Add a `defmethod fetch` in `core.clj` returning `{:title :description :data :params}`, where `:data` is the entry seq and `:params` records `{:method :url}`. Add the keyword to the default `method-options` vector in `get-feed` to include it in the fallback chain.
</important>
