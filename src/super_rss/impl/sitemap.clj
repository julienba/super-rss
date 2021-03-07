(ns super-rss.impl.sitemap
  (:require [clj-http.client :as http]
            clojure.instant
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [net.cgrand.enlive-html :as html]
            [super-rss.html :as rss.html]
            [super-rss.impl.common :as common]
            [super-rss.util :as util]))

(defn- find-sitemap-url-in-robots
  "Look in the robots.txt if there is a sitemap defined"
  [base-url]
  (let [{:keys [status body]} (http/get (str base-url "/robots.txt") {:throw-exceptions false})]
    (when (= 200 status)
      (when-let [sitemap-line (->> (string/split-lines body)
                                   (filter #(string/starts-with? % "Sitemap:"))
                                   (first))]
        (last (string/split sitemap-line #" "))))))

(defn find-sitemap-url-in-html
  "Look if the sitemap is specify in the html head"
  [base-url]
  (let [content (rss.html/get-web-page (str base-url "/"))]
    (when-let [url (->> (html/select content [:head :link])
                        (filter (fn [{:keys [attrs]}] (= "sitemap" (:rel attrs))))
                        first
                        :attrs :href)]
      (if (string/starts-with? url "/")
        (str base-url url)
        url))))

(defn find-sitemap-url [base-url]
  (or (find-sitemap-url-in-robots base-url)
      (find-sitemap-url-in-html base-url)
      ; Give a try to a classic sitemap URL
      (let [{:keys [status]} (http/get (str base-url "/sitemap.xml") {:throw-exceptions false})]
        (when (= 200 status)
          (str base-url "/sitemap.xml")))))

(defn parse-xml-string [s]
  (xml/parse (java.io.ByteArrayInputStream. (.getBytes s))))

(defn- fetch-sitemap [url]
  (let [{:keys [status body]} (http/get url)]
    (when (= 200 status)
      (let [content-list (:content (parse-xml-string body))
            url-list (for [{:keys [content]} content-list
                           :let [url     (->> content (filter #(= :loc (:tag %))) first :content first)
                                 lastmod (->> content (filter #(= :lastmod (:tag %))) first :content first)]]
                       {:url url
                        ; Sometimes the date is accurate
                        :lastmod (when lastmod
                                   (try
                                     (clojure.instant/read-instant-date lastmod)
                                     (catch Exception _)))})]
        (->> url-list
             (sort-by :lastmod)
             (reverse)
             (map :url))))))

(defn sitemap-url->urls
  [url]
  (let [urls (fetch-sitemap url)]
    (mapcat #(if (string/ends-with? % ".xml") ; sitemap can be in .gz, which is not supported right now
               (fetch-sitemap %) ; We also parse the first "inner" sitemap (ie. a sitemap referring another sitemap)
               [%])
            urls)))

(defn poor-man-rss
  "Try to create and RSS feed using the sitemap.
   Limitation:
    - page has to match common/blog-url?
    - we only read 30 pages (arbitrary limit to avoid crawling too much of a website)"
  [url {:keys [handlers]}]
  (let [base-url    (util/get-base-url url)
        sitemap-url (find-sitemap-url base-url)]
    (cond
      (nil? sitemap-url)
      (log/debug "Cannot find sitemap for %s" url)

      (not (string/ends-with? sitemap-url ".xml"))
      (log/infof "Sitemap type not supported %s" sitemap-url)

      :else

      (when-let [sitemap-urls (sitemap-url->urls sitemap-url)]
        (let [post-urls (->> sitemap-urls
                             (remove nil?)
                             (filter common/blog-url?))]
          (->> post-urls
               (remove #(get #{"/news/" "/blog/"} %))
               ; Remove page already ingest
               (remove (fn [url] (if-let [f (:already-ingest? handlers)]
                                   (f url)
                                   false)))
               (take 30) ; Limit the amount of page to crawl
               (map #(try (rss.html/extract-html-meta %)
                       (catch Exception e
                         ; Sitemap can contains non working page (404, redirect loop, etc)
                         (log/infof e "Fail to parse URL %s" %))))
               (remove nil?)))))))
