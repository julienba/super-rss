(ns super-rss.impl.normal
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [remus :as remus]
            [super-rss.html :as rss.html]
            [super-rss.util :as util]))

(defn find-feed-url
  [website-url]
  (let [content  (rss.html/get-web-page website-url {"User-Agent" "super-rss rss-reader"})
        feed-url (->> (html/select content [:link])
                      (filter #(get #{"application/atom+xml" "application/rss+xml" "text/xml"}
                                    (get-in % [:attrs :type])))
                      first
                      :attrs :href)]
    (when feed-url
      (util/url->absolute-url (util/get-base-url website-url) feed-url))))

(defn fetch-rss
  "Fetch feed, works with all RSS format"
  [url timeout]
  (try
    (let [{:keys [feed]} (remus/parse-url url {:insecure? true
                                               :connection-timeout timeout
                                               :headers {"User-Agent" "super-rss rss-reader"}})]
      {:title       (some-> (:title feed) (string/trim))
       :description (some-> (:description feed) (string/trim))
       :entries     (:entries feed)})
    (catch clojure.lang.ExceptionInfo e
      (let [response (ex-data e)]
        (log/errorf "Fail to fetch url %s %s" url response)))
    (catch Exception e
      (log/errorf e "Fail to fetch url %s" url))))
