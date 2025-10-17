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

(defn pagination-link?
  "Check if a link is a pagination link based on URL pattern and text"
  [href title]
  (let [href-lower (string/lower-case href)
        title-lower (string/lower-case (or title ""))
        pagination-url-patterns [#"/page/\d+"
                                 #"/p/\d+"
                                 #"/posts/\d+"
                                 #"/articles/\d+"
                                 #"/blog/\d+"
                                 #"/news/\d+"
                                 #"/\d+$"
                                 #"/page-\d+"
                                 #"/p-\d+"
                                 #"\?page=\d+"]
        pagination-text-patterns [#"^next\s*[→>]?$"
                                  #"^previous\s*[←<]?$"
                                  #"^page\s*\d+$"
                                  #"^\d+$"
                                  #"^older\s*posts?$"
                                  #"^newer\s*posts?$"
                                  #"^load\s*more$"
                                  #"^show\s*more$"
                                  #"^more\s*posts?$"
                                  #"^earlier\s*posts?$"
                                  #"^later\s*posts?$"]]
    (or
     (some #(re-find % href-lower) pagination-url-patterns)
     (some #(re-find % title-lower) pagination-text-patterns))))

(defn find-all-links
  [root-url content]
  (->> (hs/select (hs/child (hs/tag :a)) content)
       (remove #(= "nofollow" (get-in % [:attrs :rel]))) ; be a good Netizen
       (map #(get-in % [:attrs :href]))
       (common/cleanup-urls root-url)))

(def min-length-anchor-href 10)
(def min-length-anchor-content 12)

(defn- clean-title
  "Clean and normalize title text by removing common unwanted patterns"
  [s]
  (some-> s
          string/trim
          ;; Remove newlines and normalize whitespace
          (string/replace #"\n" "")
          (string/replace #"\s+" " ")
          ;; Remove common category/navigation patterns (only specific ones)
          (string/replace #"\s*[-→]\s*[-→]\s*(Product|Insight|Blog|News|Articles)$" "") ; " - → - Product" or " - → - Insight"
          (string/replace #"\s*→\s*(Product|Insight|Blog|News|Articles)$" "") ; " → Product" or " → Insight"
          ;; Remove date and author patterns (more comprehensive)
          (string/replace #"\s*[-–]\s*\d{1,2}\s+[A-Za-z]{3,4}\s+\d{4}\s*[-–]\s*[A-Za-z\s]+$" "") ; " - 13 Jan 2022 - Bernard Labno"
          (string/replace #"\s*\d{1,2}\s+[A-Za-z]{3,4}\s+\d{4}\s+[A-Za-z\s]+$" "") ; "13 Jan 2022 Bernard Labno"
          (string/replace #"\s*\d{1,2}\s+[A-Za-z]{3,4}\s+\d{4}$" "") ; "13 Jan 2022"
          ;; Remove author patterns that might be concatenated
          (string/replace #"\s+[A-Za-z\s]+\s+\d{1,2}\s+[A-Za-z]{3,4}\s+\d{4}$" "") ; "Author Name 13 Jan 2022"
          (string/replace #"\s+\d{1,2}\s+[A-Za-z]{3,4}\s+\d{4}\s+[A-Za-z\s]+$" "") ; "13 Jan 2022 Author Name"
          ;; Remove common navigation text
          (string/replace #"\s*Read more\s*$" "")
          (string/replace #"\s*Continue reading\s*$" "")
          (string/replace #"\s*Learn more\s*$" "")
          ;; Remove common blog navigation patterns (only at the end)
          (string/replace #"\s*[-–]\s*Blog\s*$" "")
          (string/replace #"\s*[-–]\s*News\s*$" "")
          (string/replace #"\s*[-–]\s*Articles\s*$" "")
          ;; Clean up any remaining extra whitespace
          string/trim
          ;; Don't return empty strings
          (as-> s (when-not (string/blank? s) s))))

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
  "Look for title elements that are siblings or children of the anchor's parent"
  [anchor]
  (let [anchor-parent (when anchor (-> anchor :content first))
        siblings (when anchor-parent (html/select anchor-parent [:h1 :h2 :h3 :h4 :h5]))
        children (html/select anchor [:h1 :h2 :h3 :h4 :h5])]
    (->> (concat siblings children)
         (map html/text)
         (map string/trim)
         (remove string/blank?)
         first)))

(defn- extract-title-from-url
  "Extract a readable title from a URL path"
  [url]
  (when url
    (let [path (-> url
                   (string/replace #"^https?://[^/]+" "")
                   (string/replace #"^/" "")
                   (string/replace #"/$" ""))
          segments (string/split path #"/")
          last-segment (last segments)]
      (when (and last-segment (not= last-segment ""))
        (-> last-segment
            (string/replace #"-" " ")
            (string/replace #"_" " ")
            (string/replace #"\.(html?|php|asp|aspx|jsp|cgi|pl|py|rb|js|css|xml|json|txt|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|tar|gz|rar|7z|mp3|mp4|avi|mov|wmv|flv|webm|ogg|wav|aac|flac|m4a|wma|ra|rm|rmvb|mkv|divx|xvid|3gp|3g2|amr|mid|midi|au|snd|aiff|aif|aifc|swf|fla|as|as3|mxml|swc|air|ipa|apk|deb|rpm|dmg|pkg|msi|exe|app|dll|so|dylib|a|lib|o|obj|class|jar|war|ear|sar|nar|kar|par|rar|zip|7z|tar|gz|bz2|xz|lzma|lz|lzo|lz4|zstd|lzop|lha|lzh|arc|arj|cab|msi|exe|com|bat|cmd|scr|pif|vbs|js|wsf|hta|chm|hlp|reg|inf|ini|cfg|conf|log|tmp|temp|bak|old|orig|save|swp|swo|swn)$" "")
            string/trim
            (as-> s (when-not (string/blank? s) s)))))))

(defn extract-feed-information [post-node]
  (let [anchors (->> (html/select post-node [:a])
                     (remove #(> min-length-anchor-href (count (get-in % [:attrs :href] ""))))
                     (remove #(re-find common/ignore-href-pattern (get-in % [:attrs :href] ""))))
        main-anchor (if (= 1 (count anchors))
                      (first anchors)
                      (->> anchors
                           (remove #(map? (-> % :content first)))
                           (sort comp-content-length)
                           last))
        link (-> main-anchor :attrs :href)
        content-title (-> main-anchor html/text)
        anchor-title (find-title-in-anchor main-anchor)
        nearby-title (find-title-near-anchor main-anchor)
        extra-content (search-for-extra-content post-node)
        url-title (extract-title-from-url link)
        final-title (or (clean-title anchor-title)
                        (clean-title nearby-title)
                        (if (string/blank? content-title)
                          (clean-title (:title extra-content))
                          (clean-title content-title))
                        url-title)]
    (when-not (string/blank? link)
      (when-not (pagination-link? link final-title)
        {:link link
         :title final-title
         :description (clean-string (:description extra-content))
         :published-date (when (:date extra-content)
                           (date/local-date->date (:date extra-content)))}))))

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

;; TODO Once found the content should be update to not have to parse the entire structure again
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
  (let [content (rss.html/fetch-hickory url {"User-Agent" "super-rss poor-man-rss"})
        root-url (common/get-root-url url)
        all-links (->> (find-all-links root-url content)
                       (remove #(= url %)))]
    (find-list root-url content all-links)))
