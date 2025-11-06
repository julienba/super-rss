(ns super-rss.impl.normal
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [remus :as remus]
            [super-rss.html :as rss.html]
            [super-rss.util :as util]))

(defn- feed-url->absolute-feed-url [website-url feed-url]
  (util/url->absolute-url (util/get-base-url website-url) feed-url))

(defn- find-feed-url' [website-url content]
  (if-let [correct-feed-url (->> (html/select content [:link])
                                 (filter #(get #{"application/atom+xml" "application/rss+xml" "text/xml"}
                                               (get-in % [:attrs :type])))
                                 first
                                 :attrs :href)]
    correct-feed-url
    (when-let [link-feed-url (->> (html/select content [:a])
                                  (filter (fn [node]
                                            (and (re-find #"RSS" (apply str (:content node)))
                                                 (get-in node [:attrs :href])
                                                 (or (string/starts-with? (get-in node [:attrs :href]) "/")
                                                     (string/starts-with? (get-in node [:attrs :href]) (util/get-base-url website-url))))))
                                  first
                                  :attrs :href)]
      link-feed-url)))

(defn find-feed-url
  [website-url]
  (let [content (rss.html/fetch website-url {"User-Agent" "super-rss rss-reader"})]
    (when-let [feed-url (find-feed-url' website-url content)]
      (feed-url->absolute-feed-url website-url feed-url))))

(defn fetch-rss
  "Fetch feed, works with all RSS format"
  [url {:keys [timeout throw?]}]
  (try
    (let [{:keys [feed]} (remus/parse-url url {:insecure? true
                                               :connection-timeout timeout
                                               :headers {"User-Agent" "super-rss rss-reader"}})]
      {:title       (some-> (:title feed) (string/trim))
       :description (some-> (:description feed) (string/trim))
       :entries     (:entries feed)})
    (catch clojure.lang.ExceptionInfo e
      (let [response (ex-data e)]
        (log/errorf "Fail to fetch url %s %s" url response)
        (when throw?
          (throw e))))
    (catch Exception e
      (log/errorf "Fail to fetch url %s : %s" url (ex-message e))
      (when throw?
        (throw e)))))
