(ns super-rss.impl.flat-smart-links
  "Variation of smart links that get all links, the downside being the risk of high noise"
  (:require [hickory.select :as hs]
            [super-rss.html :as rss.html]
            [super-rss.impl.common :as common]
            [super-rss.impl.smart-links :as smart-links]
            [super-rss.util :as util]))

(defn- distinct-by [f coll]
  (letfn [(step [xs seen]
            (lazy-seq
              (when-let [s (seq xs)]
                (let [x (first s)
                      val (f x)]
                  (if (contains? seen val)
                    (step (rest s) seen)
                    (cons x (step (rest s) (conj seen val))))))))]
    (step coll #{})))

(defn flat-poor-man-rss-html [url]
  (let [content (rss.html/get-hickory-web-page url)
        root-url (common/get-root-url url)
        all-links (->> (smart-links/find-all-links root-url content)
                       (remove #(= url %))
                       (remove nil?)
                       distinct)]
    (->> all-links
         (map (fn [href]
                (let [loc (first (hs/select (hs/child (hs/and (hs/tag :a)
                                                              (hs/attr :href #(= (util/url->absolute-url root-url %)
                                                                                 href))))
                                             content))]
                  (when loc
                    (smart-links/extract-feed-information loc)))))
         (remove nil?)
         (remove #(smart-links/pagination-link? (:link %) (:title %)))
         (distinct-by :link))))
