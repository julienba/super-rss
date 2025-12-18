(ns super-rss.html
  (:require [clojure.string :as string]
            [super-rss.http :as http]
            [net.cgrand.enlive-html :as html]
            [net.cgrand.xml :as xml]
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


; ~ Parsing ====================================================================

(defn- clean-title
  "Remove common noise present in HTML page"
  [title]
  (when (string? title)
    (let [[cleaned] (string/split title #"\s*\|\s*" 2)]
      (when cleaned
        (string/trim cleaned)))))

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
  (let [data (fetch url {"User-Agent" "super-rss title+description-finder"})]
    {:title       (some-> (get-page-title data) string/trim clean-title)
     :description (some-> (get-page-description data) string/trim)}))

(defn extract-html-meta
  "Return the minimal expected element for a RSS feed: title, description, published-date, link"
  [url]
  (let [data        (fetch url {"User-Agent" "super-rss build-rss-from-html"})
        date        (get-page-date data)]
    (merge (extract-simple-html-meta url)
           {:published-date (when date (date/local-date->date date))
            :link url})))

(defn text
  "Returns the text value of a node join separated by a whitespace.
   A variation of net.cgrand.enlive-html/text"
  {:tag String}
  [node]
  (cond
    (string? node) node
    (xml/tag? node) (string/join " " (map text (:content node)))
    :else ""))