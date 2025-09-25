(ns super-rss.impl.common
  (:require [clojure.string :as string])
  (:import (java.net URL)))

(def ignore-href-pattern
  (re-pattern "(?i)#|instagram|facebook|twitter|linkedin|/terms-and-privacy/|^/author|^/privacypolicy/|privacypolicy.html|^mailto:|^javascript:"))

(def ^:private post-prefix #{"/post/"
                             "/blog/"
                             "/content/"
                             "/news/"
                             "/article/"})

(defn ^:deprecated blog-url?
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

;; TODO duplicated of super-rss.util but this one does not return the `/`
(defn get-root-url [url]
  (let [url-obj (URL. url)
        host (.getHost url-obj)
        protocol (.getProtocol url-obj)]
    (str protocol "://" host "/")))

(def ^:deprecated article-prefix
  #"(?i)#|^/blog/|^blog/|^/news/|^news/|^/articles/|^articles/|^insights/")

(defn cleanup-urls
  "Cleanup urls that are not intresting and not from the same domain.
   urls are expected to be absolutes"
  [root-url urls]
  (let [filter-urls (->> urls
                         (remove string/blank?)
                         (remove #(= root-url %))
                         (remove #(= (str root-url "/") %))
                         (filter #(string/starts-with? % root-url))
                         (remove #(re-find #"(?i)#|/author/|/terms-and-privacy/|/article$|/articles$|/blog$|/blog/$|/contact$|/contact/$|/news$|/news/$|/tag$|/tag/$|/about$|/about-us$|/about-us/$|/privacypolicy|/terms-and-privacy/|javascript:|mailto:|all-posts$|all-posts/$|privacy-policy$|/page/\d+|/p/\d+|/posts/\d+|/articles/\d+|/blog/\d+|/news/\d+"
                                           %))
                         distinct)
        prefix-urls (filter #(re-find #"/blog/..*|/article/..*|/post..*|/news..*" %) filter-urls)]
    (if (<= 3 (count prefix-urls))
      prefix-urls
      filter-urls)))
