(ns super-rss.impl.smart-links
  (:require clojure.set
            [clojure.string :as string]
            [clojure.zip :as z]
            [hickory.select :as hs]
            [net.cgrand.enlive-html :as html]
            [super-rss.date :as date]
            [super-rss.html :as rss.html]
            [super-rss.impl.common :as common]
            [super-rss.util :as util]))

(defn- find-all-links
  [root-url content]
  (->> (hs/select (hs/child (hs/tag :a)) content)
       (remove #(= "nofollow" (get-in % [:attrs :rel]))) ; be a good Netizen
       (map #(get-in % [:attrs :href]))
       (common/cleanup-urls root-url)))

(def min-length-anchor-href 10)
(def min-length-anchor-content 12)

(defn- clean-string [s]
  (some-> s
          string/trim
          (string/replace #"\n" "")
          ;; Replace long empty space => side effect of not find the right element
          (string/replace #"\s\s\s+" " - ")))

(defn- search-for-extra-content
  "Try to find the title description and date"
  [content]
  (let [text-like-contents (html/select content #{[:div] [:p] [:span] [:h3] [:h4] [:h5] [:em] [:li]})
        text-contents (->> text-like-contents
                           (remove #(some map? (:content %)))
                           (map html/text)
                           (map string/trim)
                           (remove string/blank?))
        long-texts (->> text-contents
                        (filter #(< min-length-anchor-content (count %)))
                        (sort))
        date (->> text-contents
                  (map #(subs % 0 (min 50 (count %))))
                  (some #(when (date/str->date %) (date/str->date %))))]
    {:title (clean-string (first long-texts))
     :description (when (< 1 (count long-texts))
                    (->> long-texts
                         (sort-by count)
                         last
                         clean-string))
     :date date}))

(defn- comp-content-length
  [a b]
  (if (< (-> a :content first count) (-> b :content first count))
    1
    0))

(defn- find-title-in-anchor
  "Look for title elements (h1, h2, h3, h4, h5) within the anchor"
  [anchor]
  (->> (html/select anchor [:h1 :h2 :h3 :h4 :h5])
       (map html/text)
       (map string/trim)
       (remove string/blank?)
       first))

(defn- find-title-near-anchor
  [anchor]
  (let [anchor-parent (when anchor (-> anchor :content first))
        siblings (when anchor-parent (html/select anchor-parent [:h1 :h2 :h3 :h4 :h5]))
        children (html/select anchor [:h1 :h2 :h3 :h4 :h5])]
    (->> (concat siblings children)
         (map html/text)
         (map string/trim)
         (remove string/blank?)
         first)))

(defn- extract-feed-information [post-node]
  (let [anchors (->> (html/select post-node [:a])
                     (remove #(> min-length-anchor-href (count (get-in % [:attrs :href] ""))))
                     (remove #(re-find common/ignore-href-pattern (get-in % [:attrs :href] ""))))
        main-anchor (if (= 1 (count anchors))
                      (first anchors)
                      ;; Take the anchor with the longest content
                      (->> anchors
                           (remove #(map? (-> % :content first)))
                           (sort comp-content-length)
                           last))
        link (-> main-anchor :attrs :href)
        content-title (-> main-anchor html/text)
        anchor-title (find-title-in-anchor main-anchor)
        nearby-title (find-title-near-anchor main-anchor)
        extra-content (search-for-extra-content post-node)]
    (when-not (string/blank? link)
      {:link link
       :title (or (clean-string anchor-title)
                  (clean-string nearby-title)
                  (if (string/blank? content-title)
                    (clean-string (:title extra-content))
                    (clean-string content-title)))
       :description (clean-string (:description extra-content))
       :published-date (when (:date extra-content)
                         (date/local-date->date (:date extra-content)))})))

;; Minimum number of nodes that needs to contains feed-like information
(def min-node 2)

(defn- find-list*
  "Explore parent nodes to see if one is a list of content"
  [root-url content href]
  (if-let [loc (-> (hs/select-locs (hs/child (hs/and (hs/tag :a)
                                                     (hs/attr :href #(= (util/url->absolute-url root-url %)
                                                                        href))))
                                   content)
                   first)]
    (let [childs->entry (fn [maybe-childs]
                          (->> maybe-childs
                               (filter map?)
                               (map extract-feed-information)
                               (remove nil?)))
          results0 (-> loc
                       z/up
                       z/children
                       childs->entry)
          results1 (-> loc
                       z/up z/up
                       z/children
                       childs->entry)
          results2 (-> loc
                       z/up z/up z/up
                       z/children
                       childs->entry)
          results3 (-> loc
                       z/up z/up z/up z/up
                       z/children
                       childs->entry)
          results4 (-> loc
                       z/up z/up z/up z/up z/up
                       z/children
                       childs->entry)]
      (cond
        (< min-node (count results0)) results0
        (< min-node (count results1)) results1
        (< min-node (count results2)) results2
        (< min-node (count results3)) results3
        (< min-node (count results4)) results4
        :else nil))
    (throw (ex-info "The href cannot be found in the document"
                    {:href href
                     :root-url root-url}))))

;; TODO once found the content should be update to not have to parse the entire structure again
;; find-list* should return the modify content as well
(defn- find-list [source-url content hrefs]
  (loop [href-set (set hrefs)
         explored #{}
         results []]
    (cond
      (empty? href-set)
      (->> results
           (group-by :link)
           vals
           (map first))
      (get explored (first href-set))
      (recur (disj href-set (first href-set)) explored results)

      :else (let [founds (->> (find-list* source-url content (first href-set))
                              (filter #(get href-set (:link %))))
                  href-found-set (set (map :link founds))]
              (if (empty? founds)
                (recur (disj href-set (first href-set))
                       (conj explored (first href-set))
                       results)
                (recur (clojure.set/difference href-set href-found-set)
                       (conj explored (first href-set))
                       (concat results founds)))))))

(defn poor-man-rss-html [url]
  (let [content (rss.html/get-hickory-web-page url)
        root-url (common/get-root-url url)
        all-links (->> (find-all-links root-url content)
                       (remove #(= url %)))]
    (find-list root-url content all-links)))
