(ns super-rss.impl.sitemap
  "Best effort to transform a sitemap into an RSS feed.
   This idea is to continiously improve it by adding more and more heuristics,
   as the specification (https://www.sitemaps.org/protocol.html) is not always followed and the matching by prefix is lackluster."
  (:require clojure.instant
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [net.cgrand.enlive-html :as html]
            [super-rss.robots-txt :as robots-txt]
            [super-rss.html :as rss.html]
            [super-rss.http :as http]
            [super-rss.impl.common :as common]
            [super-rss.util :as util]))

(defn- find-sitemap-url-in-robots-contents [base-url robots-vec]
  (let [urls (map :value (filter #(re-find #"^Sitemap$|^sitemap$" (:key %)) robots-vec))
        _ (when (< 1 (count urls))
            (log/infof "Multiple sitemaps found %s" (pr-str urls)))
        best-url (first (filter #(re-find common/article-prefix (string/replace-first % base-url "")) urls))]
    (if best-url
      best-url
      (first urls))))

(defn- find-sitemap-url-in-robots
  "Look in the robots.txt if there is a sitemap defined"
  [base-url]
  (when-let [robots-vec (robots-txt/get-robots-txt base-url)]
    (find-sitemap-url-in-robots-contents base-url robots-vec)))

(defn- find-sitemap-url-in-html
  "Look if the sitemap is specify in the html head"
  [base-url]
  (let [content (rss.html/fetch (str base-url "/") {"User-Agent" "super-rss sitemap-finder"})]
    (when-let [url (->> (html/select content [:head :link])
                        (filter (fn [{:keys [attrs]}] (= "sitemap" (:rel attrs))))
                        first
                        :attrs :href)]
      (if (string/starts-with? url "/")
        (str base-url url)
        url))))

(defn- find-sitemap-url [base-url]
  (or (find-sitemap-url-in-robots base-url)
      (find-sitemap-url-in-html base-url)
      ; Give a try to a classic sitemap URL
      (let [{:keys [status]} (http/get (str base-url "/sitemap.xml") {:throw-on-error false})]
        (when (= 200 status)
          (str base-url "/sitemap.xml")))))

(defn- parse-xml-string [s]
  (xml/parse (java.io.ByteArrayInputStream. (.getBytes s))))

(defn- fetch-sitemap [url]
  (let [{:keys [status body]} (http/get url {:throw false
                                              :headers {"User-Agent" "super-rss sitemap-reader"}})]
    (when (= 200 status)
      (let [content-list (:content (parse-xml-string body))
            url-list (for [{:keys [content]} content-list
                           :let [url (->> content (filter #(= :loc (:tag %))) first :content first)
                                 lastmod (->> content (filter #(= :lastmod (:tag %))) first :content first)]]
                       {:url url
                        ; Sometimes the date is accurate, sometimes no
                        :lastmod (when lastmod
                                   (try
                                     (clojure.instant/read-instant-date lastmod)
                                     (catch Exception _)))})]
        (->> url-list
             (sort-by :lastmod)
             (reverse))))))

(defn- sitemap-url->sitemap-contents
  [url]
  (let [sitemap-contents (fetch-sitemap url)]
    (mapcat #(if (string/ends-with? (:url %) ".xml") ; sitemap can be in .gz, which is not supported right now
               (fetch-sitemap (:url %)) ; It also parse the first "inner" sitemap (ie. a sitemap referring another sitemap)
               [%])
            sitemap-contents)))

(def page-crawl-limit 3)

(defn- url->title
  "Extract a title from a URL path"
  [url]
  (when url
    (try
      (let [url-str (string/trim url)
            ;; Remove protocol and domain
            path (-> url-str
                     (string/replace #"^https?://[^/]+" "")
                     (string/replace #"[?#].*$" "")) ; Remove query params and fragments
            ;; Get the last meaningful path segment
            segments (-> path
                         (string/replace #"/$" "") ; Remove trailing slash
                         (string/split #"/"))
            last-segment (or (last (remove string/blank? segments)) "")
            ;; Clean up the segment
            cleaned (-> last-segment
                        (string/replace #"\.html?$" "") ; Remove .html or .htm
                        (string/replace #"\.[a-z]+$" "") ; Remove other extensions
                        (string/replace #"[-_]" " ") ; Replace separators with spaces
                        string/trim)]
        (when (seq cleaned)
          ;; Capitalize each word
          (->> (string/split cleaned #"\s+")
               (map string/capitalize)
               (string/join " "))))
      (catch Exception _
        nil))))

(defn- cleanup-titles
  "When all titles are the same use the URL to 'infer' the title.
   By infer I mean than an URL like https://www.ohpen.com/latest-insights/article/ohpen-and-ortec-finance-join-forces-to-bring-innovation-to-pensions
   should become 'Ohpen and ortec finance join forces to bring innovation to pensions"
  [sitemap-data]
  (when (seq sitemap-data)
    (let [titles (map :title sitemap-data)
          all-same? (or (every? nil? titles)
                        (every? string/blank? titles)
                        (and (seq titles)
                             (apply = (remove nil? titles))))]
      (if all-same?
        (map (fn [item]
               (if-let [url-title (url->title (:link item))]
                 (assoc item :title url-title)
                 item))
             sitemap-data)
        sitemap-data))))

(defn poor-man-rss
  "Try to create an RSS feed using the sitemap.
   Limitation:
   - page has to match common/blog-url?
   - read only the first pages (arbitrary limit to avoid crawling too much of a website)"
  [url {:keys [handlers]}]
  (let [base-url (util/get-base-url url)
        sitemap-url (if (string/ends-with? url ".xml")
                      url
                      (find-sitemap-url base-url))
        data (cond
               (nil? sitemap-url)
               (log/debug "Cannot find sitemap for %s" url)

               ; TODO if it's a folder it might crash but let's implement it the next time
               ;; I encounter it or dig the protocol https://www.sitemaps.org/protocol.html
               ;;  (not (string/ends-with? sitemap-url ".xml"))
               ;;  (log/infof "Sitemap type not supported %s" sitemap-url)

               :else

               (when-let [sitemap-contents (sitemap-url->sitemap-contents sitemap-url)]
                 (let [sitemap-contents-filter (->> sitemap-contents
                                                    (remove #(nil? (:url %)))
                                                    (filter #(re-find common/article-prefix (string/replace-first (:url %) base-url ""))))
                       urls-prefixed? (< 2 (count sitemap-contents-filter))
                       clean-sitemap-contents (if urls-prefixed?
                                                sitemap-contents-filter
                                                sitemap-contents)
                       clean-sitemap-contents (->> clean-sitemap-contents
                                                   (remove nil?)
                                                   (remove #(nil? (:url %)))
                                                   (filter #(common/blog-url? (:url %))))]
                   (->> clean-sitemap-contents
                        (map #(update % :url (fn [url] (some-> url string/trim))))
                        ; Remove page already ingest
                        (remove (fn [{:keys [url]}]
                                  (if-let [f (:already-ingest? handlers)]
                                    (f url)
                                    false)))
                        (take page-crawl-limit)
                        (map (fn [{:keys [url lastmod]}]
                               (try (cond-> (rss.html/extract-html-meta url)
                                      lastmod (assoc :published-date lastmod))
                                    (catch Exception e
                                      ; Sitemap can contains non-working page (404, redirect loop, etc)
                                      (log/infof e "Fail to parse URL %s" url)))))
                        (remove nil?)
                        (cleanup-titles)))))]
    {:data data
     :url sitemap-url}))

;; Good regresion test:
;; - "https://design.google": return a 308 and articles are prefix by the unusual '/library'
;; - https://rasa.com robots.txt use sitemap instead of Sitemap and there are multiple sitemap

