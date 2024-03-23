(ns super-rss.impl.common
  (:require [clojure.string :as string])
  (:import (java.net URL)))

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

;; TODO duplicated of super-rss.util but this one does nto return the `/`
(defn get-root-url [url]
  (let [url-obj (URL. url)
        host (.getHost url-obj)
        protocol (.getProtocol url-obj)]
    (str protocol "://" host "/")))

(def ^:private bad-pages
  #{"/blog/" "/news/" "/category/" "/author/" "/tags/"
    "/about"
    "/contact"})

(def ignore-href-pattern
  (re-pattern "(?i)#|instagram|facebook|twitter|linkedin|/terms-and-privacy/|^/author|^/privacypolicy/|privacypolicy.html|^mailto:|^javascript:"))

(def article-prefix
  #"(?i)#|^/blog/|^blog/|^/news/|^news/|^/articles/|^articles/")

(defn- clean-by-prefix [urls]
  (let [urls-prefixed (filter #(re-find article-prefix %) urls)]
    (if (< 2 (count urls-prefixed))
      urls-prefixed
      urls)))

(defn cleanup-urls [root-url urls]
  (->> urls
       (remove string/blank?)
       (remove #(= "/" %))
       (map #(string/replace-first % root-url "/"))
       (remove #(re-find ignore-href-pattern %))
       (clean-by-prefix)
       (remove #(get bad-pages %))
       distinct
       (map #(str root-url
                  (if (string/starts-with? % "/")
                    (subs % 1)
                    %)))))

#_(cleanup-urls "http://a.com/" ["/"])

#_(cleanup-urls "http://a.com/" ["http://a.com/blog"
                               "http://a.com/blog/"
                               "http://a.com/random"
                               "http://a.com/random/more"
                               "http://a.com/blog/article1"
                               "http://a.com/blog/article2"
                               "/blog"
                               "/blog/"
                               "/random"
                               "/random/more"
                               "/blog/article1"
                               "/blog/article2"
                               "/"
                               "blog/article0"])
