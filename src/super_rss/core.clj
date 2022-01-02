(ns super-rss.core
  (:require clojure.instant
            [clojure.tools.logging  :as log]
            [super-rss.impl.normal  :as impl.normal]
            [super-rss.impl.sitemap :as impl.sitemap]
            [super-rss.impl.links   :as impl.links]))

(defmulti fetch
  "Different implementation for creating an RSS feed"
  (fn [method _url _opts] method))

(defmethod fetch :direct-rss [_ url {:keys [timeout]}]
  (when-let [result (impl.normal/fetch-rss url timeout)]
    {:title (:title result)
     :description (:description result)
     :data (:entries result)}))

(defmethod fetch :find-rss-url [_ url {:keys [timeout]}]
  (when-let [feed-url (impl.normal/find-feed-url url)]
    {:data (impl.normal/fetch-rss feed-url timeout)
     :param feed-url}))

(defmethod fetch :page-links [_ url opts]
  {:data (impl.links/poor-man-rss-html url opts)})

(defmethod fetch :sitemap [_ url opts]
  {:data (impl.sitemap/poor-man-rss url opts)})

(defmethod fetch :default [method & _]
  (log/errorf "Fetch method %s don't exist" method))

(defn get-feed
  "Fetch fetch with different strategies, from the normal one to the \"hacky\" one.
   `method:` when you know which method to use to get a feed
   `method-options:` list of strategy
   Return a map of `:data` with the RSS feed and `:method` with the method used to retrieve the feed."
  [url
   {:keys [timeout method method-options]
    :or {timeout 10000 method-options [:find-rss-url :sitemap :page-links]}}
   {:keys [_already-ingest?] :as handler-fns}]
  (if method
    (let [method-keyword (if (vector? method) (first method) method)]
      (when-let [result (fetch method-keyword url {:timeout timeout :handlers handler-fns})]
        {:method method-keyword
         :title (:title result)
         :description (:description result)
         :result (:data result)}))
    (loop [[method & methods] method-options]
      (if (nil? method)
        nil
        (let [{:keys [data title description param] :as result} (fetch method url {:timeout timeout :handlers handler-fns})]
          (log/infof "Fetch %s using method %s" url method)
          (if (or (nil? result) (empty? data))
            (recur methods)
            {:method [method param]
             :title title
             :description description
             :result data}))))))
