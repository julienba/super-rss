(ns super-rss.core
  (:require clojure.instant
            [clojure.tools.logging :as log]
            [super-rss.impl.normal :as impl.normal]
            [super-rss.impl.links :as impl.links]
            [super-rss.impl.sitemap :as impl.sitemap]
            [super-rss.impl.smart-links :as impl.smart-links]))

(defmulti fetch
  "Different implementation for creating an RSS feed"
  (fn [method _url _opts] method))

(defmethod fetch :direct-rss [_ url {:keys [timeout]}]
  (when-let [result (impl.normal/fetch-rss url timeout)]
    {:title (:title result)
     :description (:description result)
     :data (:entries result)
     :params {:method :direct-rss}}))

(defmethod fetch :find-rss-url [_ url {:keys [timeout]}]
  (when-let [feed-url (impl.normal/find-feed-url url)]
    (when-let [result (impl.normal/fetch-rss feed-url timeout)]
      {:title (:title result)
       :description (:description result)
       :data (:entries result)
       :params {:method :direct-rss
                :url feed-url}})))

;; deprecated
(defmethod fetch :page-links [_ url opts]
  {:data (impl.links/poor-man-rss-html url opts)
   :params {:method :page-links}})


(defmethod fetch :smart-links [_ url _]
  {:data (impl.smart-links/poor-man-rss-html url)
   :params {:method :page-links}})

(defmethod fetch :sitemap [_ url opts]
  (let [result (impl.sitemap/poor-man-rss url opts)]
    {:data (:data result)
     :params {:method :sitemap
              :url (:url result)}}))

(defmethod fetch :default [method & _]
  (log/errorf "Fetch method %s don't exist" method))

(defn get-feed
  "Fetch fetch with different strategies, from the normal one to the \"hacky\" one.
   `method:` when you know which method to use to get a feed
   `method-options:` list of strategy
   Return a map of `:data` with the RSS feed and `:method` with the method used to retrieve the feed."
  [url
   {:keys [timeout method method-options]
    :or {timeout 10000 method-options [:find-rss-url :sitemap #_:page-links :smart-links]}}
   {:keys [_already-ingest?] :as handler-fns}]
  (if method
    (when-let [result (fetch method url {:timeout timeout :handlers handler-fns})]
      {:params (:params result)
       :title (:title result)
       :description (:description result)
       :results (:data result)})
    (loop [[method & methods] method-options]
      (if (nil? method)
        nil
        (let [{:keys [data title description params] :as result} (fetch method url {:timeout timeout :handlers handler-fns})]
          (log/infof "Fetch %s using method %s" url method)
          (if (or (nil? result) (empty? data))
            (recur methods)
            {:params params
             :title title
             :description description
             :results data}))))))
