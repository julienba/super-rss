(ns super-rss.impl.normal
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [remus :as remus]
            [super-rss.html :as rss.html]
            [super-rss.http :as http]
            [super-rss.util :as util]))

(defn valid-rss-response?
  "Check if HTTP response looks like a valid RSS/Atom feed.
   Returns true if Content-Type indicates XML/RSS/Atom and body starts with XML."
  [{:keys [status headers body]}]
  (and (= 200 status)
       (re-find #"(?i)(xml|rss|atom)" (get headers "content-type" ""))
       (when body
         (re-find #"(?i)^\s*(<\?xml|<rss|<feed|<RDF)" body))))

(defn validate-rss-url
  "Check if a URL returns valid RSS content. Returns the URL if valid, nil otherwise."
  [url {:keys [timeout]}]
  (try
    (let [response (http/get url {:timeout-ms (or timeout 10000)
                                  :headers {"User-Agent" "super-rss rss-validator"}})]
      (if (valid-rss-response? response)
        url
        (do
          (log/infof "RSS URL %s does not return valid RSS (content-type: %s)"
                     url (get-in response [:headers "content-type"]))
          nil)))
    (catch Exception e
      (log/debugf "Failed to validate RSS URL %s: %s" url (ex-message e))
      nil)))

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
  "Find and validate RSS feed URL from a website.
   Returns the absolute feed URL if valid, nil otherwise."
  [website-url {:keys [_timeout] :as opts}]
  (try
    (let [content (rss.html/fetch website-url {"User-Agent" "super-rss rss-reader"})]
      (when-let [feed-url (find-feed-url' website-url content)]
        (let [absolute-url (feed-url->absolute-feed-url website-url feed-url)]
          (validate-rss-url absolute-url opts))))
    (catch Exception e
      (log/debugf "Failed to find feed URL for %s: %s" website-url (ex-message e))
      nil)))

(defn fetch-rss
  "Fetch feed, works with all RSS format"
  [url {:keys [timeout throw?]}]
  (try
    (let [{:keys [feed]} (remus/parse-url url {:insecure? true
                                               :connection-timeout timeout
                                               :headers {"User-Agent" "super-rss rss-reader"}})]
      {:title (some-> (:title feed) (string/trim))
       :description (some-> (:description feed) (string/trim))
       :entries (:entries feed)})
    (catch clojure.lang.ExceptionInfo e
      (let [response (ex-data e)]
        (log/errorf "Fail to fetch url %s %s" url response)
        (when throw?
          (throw e))))
    (catch Exception e
      (log/errorf "Fail to fetch url %s : %s" url (ex-message e))
      (when throw?
        (throw e)))))
