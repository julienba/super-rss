(ns super-rss.impl.normal
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [remus :as remus]
            [super-rss.html :as rss.html]
            [super-rss.http :as http]
            [super-rss.util :as util])
  (:import [java.io ByteArrayInputStream]))

(defn- body-looks-like-rss?
  "Check if body content clearly looks like an RSS/Atom/RDF feed.
   More specific than just checking for XML declaration - validates actual feed elements.
   Used as fallback when content-type is incorrect (e.g., text/html for RSS)."
  [body]
  (when body
    ;; Check for actual RSS/Atom/RDF elements, not just XML declaration
    (re-find #"(?i)<(rss\b|feed\s+xmlns|RDF\s+xmlns)" body)))

(defn valid-rss-response?
  "Check if HTTP response looks like a valid RSS/Atom feed.
   Returns true if:
   - Status is 200, AND
   - Content-Type indicates XML/RSS/Atom with valid XML body, OR
   - Body clearly looks like RSS/Atom feed (fallback for misconfigured servers)"
  [{:keys [status headers body]}]
  (and (= 200 status)
       (or
        ;; Primary: Content-type indicates RSS/XML AND body starts with XML
        (and (re-find #"(?i)(xml|rss|atom)" (get headers "content-type" ""))
             (when body
               (re-find #"(?i)^\s*(<\?xml|<rss|<feed|<RDF)" body)))
        ;; Fallback: Body clearly looks like RSS/Atom (for wrong content-type)
        (body-looks-like-rss? body))))

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
                                            (and (re-find #"(?i)RSS" (apply str (:content node)))
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

(defn- parse-rss-from-body
  "Parse RSS feed from body string. Used as fallback when server returns
   wrong content-type but body contains valid RSS.
   Note: remus/parse returns flat map (not wrapped in :feed like parse-url)."
  [body]
  (when (body-looks-like-rss? body)
    (let [stream (ByteArrayInputStream. (.getBytes body "UTF-8"))
          feed (remus/parse stream)]
      {:title (some-> (:title feed) (string/trim))
       :description (some-> (:description feed) (string/trim))
       :entries (:entries feed)})))

(defn fetch-rss
  "Fetch feed, works with all RSS format.
   Falls back to manual parsing when server returns wrong content-type."
  [url {:keys [timeout throw?]}]
  (try
    (let [{:keys [feed]} (remus/parse-url url {:insecure? true
                                               :connection-timeout timeout
                                               :headers {"User-Agent" "super-rss rss-reader"}})]
      {:title (some-> (:title feed) (string/trim))
       :description (some-> (:description feed) (string/trim))
       :entries (:entries feed)})
    (catch RuntimeException e
      (let [msg (ex-message e)]
        ;; Check if this is a content-type error - try fallback parsing
        (if (and msg (string/includes? msg "Non-XML response"))
          (do
            (log/debugf "Content-type mismatch for %s, attempting body parse" url)
            (try
              (let [response (http/get url {:timeout-ms (or timeout 10000)
                                            :headers {"User-Agent" "super-rss rss-reader"}})
                    body (:body response)]
                (or (parse-rss-from-body body)
                    (do
                      (log/errorf "Fallback parse failed for %s" url)
                      (when throw? (throw e)))))
              (catch Exception fetch-e
                (log/errorf "Fallback fetch failed for %s: %s" url (ex-message fetch-e))
                (when throw? (throw e)))))
          (do
            (log/errorf "Fail to fetch url %s : %s" url msg)
            (when throw? (throw e))))))
    (catch clojure.lang.ExceptionInfo e
      (let [response (ex-data e)]
        (log/errorf "Fail to fetch url %s %s" url response)
        (when throw?
          (throw e))))
    (catch Exception e
      (log/errorf "Fail to fetch url %s : %s" url (ex-message e))
      (when throw?
        (throw e)))))
