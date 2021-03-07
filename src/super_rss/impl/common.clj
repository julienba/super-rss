(ns super-rss.impl.common
  (:require [clojure.string :as string]))

(def ^:private post-prefix #{"/post/"
                             "/blog/"
                             "/content/"
                             "/news/"
                             "/article/"})

(defn blog-url?
  "Attempt to detect URL that look like a blog post with content (ie. no category, no wrongly name page that ends in the sitemap by mistake)"
  [url]
  (and (some #(when (string/includes? url %)
                %)
             post-prefix)
       (not (string/includes? url "/category/"))
       (not (string/includes? url "/author/"))
       (not (string/includes? url "/tag/"))
       (not (string/includes? url "/tags/"))
       (not (string/ends-with? url "/blog/"))
       (not (string/ends-with? url "/news/"))
       (boolean (seq (re-find #"[a-zA-Z]+" (last (string/split url #"/")))))))
