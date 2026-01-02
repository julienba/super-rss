# Debug New Website

## Input
- `$URL` - The website URL to analyze

## Goal
Find the best method to create an RSS feed from this website using `super-rss.core/get-feed`.

## Steps

### 1. Test with existing methods
Using `clj-nrepl-eval`, try the automatic detection:
```clojure
(require '[super-rss.core :as core] :reload)
(core/get-feed "$URL" {} {})
```

If this returns results with entries (containing `:link`, `:title`, `:date`), stop here.

### 2. Manual investigation
If auto-detection fails, investigate manually:

1. **RSS feed** - Fetch the page and look for `<link rel="alternate" type="application/rss+xml">` in `<head>`
2. **robots.txt** - Check `$URL/robots.txt` for `Sitemap:` directives
3. **Direct sitemap** - Try `$URL/sitemap.xml` or `$URL/sitemap_index.xml`
4. **Page links** - Look for article-like URLs with dates/slugs

Store all curl output in `tmp/` with descriptive names (e.g., `tmp/example_robots.txt`).

### 3. Debug and plan
If a method should work but doesn't:
1. Identify the failure point in the relevant impl (`normal.clj`, `sitemap.clj`, etc.)
2. Create a plan for a **generic** fix (not site-specific)
3. Include unit tests using saved HTML/XML from `tmp/`

## Rules
- Solutions must work across many websites — no hardcoding
- DO NOT implement until explicitly asked
- All plans must include test coverage
