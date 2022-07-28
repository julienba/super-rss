(ns super-rss.impl.links
  (:require [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [super-rss.html :as rss.html]
            [super-rss.impl.common :as common]
            [super-rss.util :as util]))

(defn poor-man-rss-html
  "Retrieve all relative link starting with prefix hinting at a content post
   If some are found, crawl each page to retrieve title, description and date.
   Date is lack luster as it need to be found in a free form HTML"
  [url {:keys [handlers]}]
  (let [content  (rss.html/get-web-page url)
        base-url (util/get-base-url url)
        links    (->> (html/select content [:a])
                      (remove nil?)
                      (map #(get-in % [:attrs :href]))
                      (remove nil?)
                      (filter common/blog-url?)
                      (distinct)
                      (remove #(get #{"/news/" "/blog/"} %))
                      ; If more than that there is probably something wrong
                      (take 30))]
    (->> links
         (map #(str base-url %))
         (remove (fn removed-already-ingested-page [url]
                   (if-let [f (:already-ingest? handlers)]
                     (f url)
                     false)))
         (map #(try (rss.html/extract-html-meta %)
                 (catch Exception e
                   (log/infof e "Fail to extract %s" %))))
         (remove nil?))))
