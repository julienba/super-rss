(ns super-rss.html
  (:require [clojure.core.cache :as cache]
            [clojure.string :as string]
            [clj-http.client :as http]
            [hickory.core :as h]
            [hickory.select :as hs]
            [net.cgrand.enlive-html :as html]
            [super-rss.date :as date]))

(def headers {"User-Agent" "super-rss"})

(defn fetch
  "Fetch an url and return and enlive version of the body"
  [url]
  (when-let [html (:body (http/get url))]
    (let [data (html/html-snippet html {:headers headers})]
      data)))

(defn fetch-hickory
  "Fetch an url and return and enlive version of the body"
  [url]
  (when-let [body (:body (http/get url {:headers headers}))]
    (when-let [content (some-> body h/parse h/as-hickory)]
      (first (hs/select (hs/child (hs/tag :html)) content)))))

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

(defn ^:deprecated get-web-page
  "Fetch a url and cache the result.
   Use caching to limit the amount of external reading.
   Deprecated in favor of the hickory version"
  [url]
  (get-web-page* url url fetch))

(defn get-hickory-web-page
  "Fetch a url and cache the result.
   Use caching to limit the amount of external reading."
  [url]
  (get-web-page* (str "hickory-" url) url fetch-hickory))

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
  "Return the title and desription of a webpage"
  [url]
  (let [data (get-web-page url)]
    {:title       (get-page-title data)
     :description (get-page-description data)}))

(defn extract-html-meta
  "Return the minimal expected element for a RSS feed: title, description, published-date, link"
  [url]
  (let [data        (get-web-page url)
        date        (get-page-date data)]
    (merge (extract-simple-html-meta url)
           {:published-date (when date (date/local-date->date date))
            :link url})))
