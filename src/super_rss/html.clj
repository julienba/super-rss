(ns super-rss.html
  (:require [clojure.core.cache :as cache]
            [clojure.string :as string]
            [babashka.http-client :as http]
            [net.cgrand.enlive-html :as html]
            [super-rss.date :as date]
            [super-rss.hickory-zipper :as hickory-zipper]))

(defn fetch
  "Fetch an url and return and enlive version of the body"
  [url headers]
  (when-let [html (:body (http/get url {:headers headers}))]
    (let [data (html/html-snippet html)]
      data)))

(defn- body->content-tree [body root-url]
  (hickory-zipper/cleanup-anchors (hickory-zipper/string->zipper body) root-url))

(defn fetch-hickory
  "Fetch an url and return and enlive version of the body"
  [url http-headers]
  (when-let [body (:body (http/get url {:headers http-headers}))]
    (body->content-tree body url)))

; ~ Cache ======================================================================
(defn- create-cache []
  (cache/ttl-cache-factory {} :ttl (* 24 60 1000)))

(def C (atom (create-cache)))

(defn reset-cache! []
  (reset! C (create-cache))
  nil)

(defn- get-web-page* [cache-key url run-fn]
  (get
    (if (cache/has? @C cache-key)
      (cache/hit @C cache-key)
      (swap! C #(cache/miss % cache-key (run-fn url))))
    cache-key))

(defn get-web-page
  "Fetch a url and cache the result.
   Use caching to limit the amount of external reading."
  [url http-headers]
  (get-web-page* url url #(fetch % http-headers)))

(defn get-hickory-web-page
  "Fetch a url and cache the result.
   Use caching to limit the amount of external reading."
  [url http-headers]
  (get-web-page* (str "hickory-" url) url #(fetch-hickory % http-headers)))

; ~ Parsing ====================================================================

(defn get-page-title [content]
  (->> (html/select content [:title])
       first
       html/text))

(defn get-page-description [content]
  (->> (html/select content [:meta])
       (filter (fn [{:keys [attrs]}] (or (= "description"    (:name attrs))
                                         (= "og:description" (:property attrs)))))
       first
       :attrs
       :content))

(defn get-page-date
  "Search for a text element containing what looks like a date"
  [content]
  (->> (html/select content #{[:div] [:p] [:span]})
       (map html/text)
       (remove empty?)
       (map string/trim)
       (map #(subs % 0 (min 50 (count %))))
       (some #(when (date/str->date %) (date/str->date %)))))

(defn extract-simple-html-meta
  "Return the title and description of a webpage"
  [url]
  (let [data (get-web-page url {"User-Agent" "super-rss title+description-finder"})]
    {:title       (get-page-title data)
     :description (get-page-description data)}))

(defn extract-html-meta
  "Return the minimal expected element for a RSS feed: title, description, published-date, link"
  [url]
  (let [data        (get-web-page url {"User-Agent" "super-rss build-rss-from-html"})
        date        (get-page-date data)]
    (merge (extract-simple-html-meta url)
           {:published-date (when date (date/local-date->date date))
            :link url})))
