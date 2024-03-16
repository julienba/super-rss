(ns super-rss.impl.smart-links
  (:require clojure.set
            [clojure.string :as string]
            [clojure.zip :as z]
            [hickory.select :as hs]
            [net.cgrand.enlive-html :as html]
            [super-rss.date :as date]
            [super-rss.html :as rss.html]))

(def ignore-href-pattern
  (re-pattern "(?i)#|instagram|facebook|twitter|linkedin|/terms-and-privacy/|/author/|mailto:|javascript:"))

(defn- find-all-links
  [content]
  (->> (hs/select (hs/child (hs/tag :a)) content)
       (remove #(let [href (get-in % [:attrs :href])]
                  (or (string/blank? href)
                      (= "/" href)
                      (re-find ignore-href-pattern href)
                      (string/starts-with? href "javascript:"))))
       (remove #(= "nofollow" (get-in % [:attrs :rel]))) ; be a good citizen
       (map #(get-in % [:attrs :href]))
       distinct))

(def min-length-anchor-href 10)
(def min-length-anchor-content 12)

(defn- clean-string [s]
  (some-> s
          string/trim
          (string/replace #"\n" "")))

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

(defn- extract-feed-information [post-node]
  (let [anchors (->> (html/select post-node [:a])
                     (remove #(> min-length-anchor-href (count (get-in % [:attrs :href] ""))))
                     (remove #(re-find ignore-href-pattern (get-in % [:attrs :href] ""))))
        main-anchor (if (= 1 (count anchors))
                      (first anchors)
                      ;; take the anchor with the longer content
                      (->> anchors
                           (remove #(map? (-> % :content first)))
                           (sort comp-content-length)
                           last))
        link (-> main-anchor :attrs :href)
        content-title (-> main-anchor html/text)
        extra-content (search-for-extra-content post-node)]
    (when-not (string/blank? link)
      {:link link
       :title (if (string/blank? content-title)
                (clean-string (:title extra-content))
                (clean-string content-title))
       :description (clean-string (:description extra-content))
       :published-date (when (:date extra-content)
                         (date/local-date->date (:date extra-content)))})))

(defn- find-list*
  "Explore parent nodes to see if one is a lsit of content"
  [content href]
  (let [loc (-> (hs/select-locs  (hs/child (hs/and (hs/tag :a)
                                                   (hs/attr :href #(= % href))))
                                 content)
                first)
        childs->entry (fn [maybe-childs]
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
      (< 2 (count results0)) results0
      (< 2 (count results1)) results1
      (< 2 (count results2)) results2
      (< 2 (count results3)) results3
      (< 2 (count results4)) results4
      :else nil)))

(defn- find-list [content hrefs]
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

      :else (let [founds (find-list* content (first href-set))
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
        all-links (find-all-links content)]
    (find-list content all-links)))
