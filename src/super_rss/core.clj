(ns super-rss.core
  (:require clojure.instant
            [clojure.tools.logging :as log]
            [super-rss.impl.flat-smart-links :as impl.flat-smart-links]
            [super-rss.impl.normal :as impl.normal]
            [super-rss.impl.sitemap :as impl.sitemap]
            [super-rss.impl.smart-links :as impl.smart-links]))

(defmulti fetch
  "Different implementation for creating an RSS feed"
  (fn [method _url _opts] method))

(defmethod fetch :direct-rss [_ url {:keys [timeout throw?]}]
  (when-let [result (impl.normal/fetch-rss url {:throw? throw? :timeout timeout})]
    {:title (:title result)
     :description (:description result)
     :data (:entries result)
     :params {:method :direct-rss
              :url url}}))

(defmethod fetch :find-rss-url [_ url {:keys [timeout throw?]}]
  (when-let [feed-url (impl.normal/find-feed-url url {:timeout timeout})]
    (when-let [result (impl.normal/fetch-rss feed-url {:throw? throw? :timeout timeout})]
      {:title (:title result)
       :description (:description result)
       :data (:entries result)
       :params {:method :direct-rss
                :url feed-url}})))

(defmethod fetch :smart-links [_ url _]
  {:data (impl.smart-links/poor-man-rss-html url)
   :params {:method :smart-links
            :url url}})

(defmethod fetch :flat-smart-links [_ url _]
  {:data (impl.flat-smart-links/flat-poor-man-rss-html url)
   :params {:method :flat-smart-links
            :url url}})

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
   {:keys [method method-options throw? timeout]
    :or {method-options [:find-rss-url :sitemap :smart-links :flat-smart-links]
         throw? false
         timeout 10000}}
   {:keys [_already-ingest?] :as handler-fns}]
  (letfn [(build-result [result]
            {:params (:params result)
             :title (:title result)
             :description (:description result)
             :results (:data result)})
          (try-method [method]
            (when-let [result (fetch method url {:handlers handler-fns
                                                 :throw? throw?
                                                 :timeout timeout})]
              (when-not (empty? (:data result))
                (log/infof "Fetch %s using method %s" url method)
                (build-result result))))]
    (if method
      (try-method method)
      (some try-method method-options))))
