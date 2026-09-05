(ns super-rss.core
  (:require clojure.instant
            [clojure.tools.logging :as log]
            [super-rss.error :as error]
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
  (when-let [feed-url (impl.normal/find-feed-url url {:timeout timeout :throw? throw?})]
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
  "Fetch a feed with different strategies, from the normal one to the \"hacky\" one.
   `method:` when you know which method to use to get a feed
   `method-options:` list of strategies, tried in order until one returns entries
   `throw?:` raise when no strategy produced a feed and at least one failed (default false)
   `timeout:` HTTP timeout in ms (default 10000)

   A strategy that throws is skipped so the next one still gets its turn: a failing
   strategy is expected, that is what the cascade is for. When every strategy failed and
   `throw?` is true, one exception is raised carrying every failure in its `ex-data`,
   see `super-rss.error/cascade-error`. With `throw?` false the run returns `nil`, which
   is also what a run where every strategy simply found nothing returns.

   Return a map of `:results` with the feed entries, `:title`, `:description` and
   `:params` holding the `:method` and `:url` that produced them."
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
            ; {:result r} on success, {:error e} on failure, nil when it found nothing
            (try
              (when-let [result (fetch method url {:handlers handler-fns
                                                   :throw? throw?
                                                   :timeout timeout})]
                (when-not (empty? (:data result))
                  (log/infof "Fetch %s using method %s" url method)
                  {:result (build-result result)}))
              (catch Exception e
                (log/debugf e "Method %s failed for %s" method url)
                {:error e})))]
    (loop [[method & more] (if method [method] method-options)
           failures []]
      (if-not method
        (when (seq failures)
          (if throw?
            (throw (error/cascade-error url failures))
            (log/warnf "No strategy produced a feed for %s: %s" url
                       (error/summarize-failures url failures))))
        (let [{:keys [result error]} (try-method method)]
          (cond
            result result
            error (recur more (conj failures {:method method :error error}))
            :else (recur more failures)))))))
