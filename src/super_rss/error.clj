(ns super-rss.error
  "Classify a failure so a caller can tell a permanently dead source from a blip.

   Every exception super-rss raises carries `ex-data`:

     {:super-rss/error :dns | :timeout | :http-status | :challenge | :parse | :unknown
      :url    \"https://example.com/feed\"
      :status 403     ; only when the failure arrived with an HTTP response
      :cause  \"message of the underlying exception\"}

   A dead domain and a one-off timeout are opposite situations: one should stop
   being retried, the other should be tried again tomorrow."
  (:require [clojure.string :as string])
  (:import [com.rometools.rome.io FeedException]
           [java.io InterruptedIOException]
           [java.net ConnectException NoRouteToHostException SocketTimeoutException UnknownHostException]
           [org.xml.sax SAXException]))

(def ^:private challenge-body-pattern
  "Cloudflare's managed-challenge interstitial."
  #"(?i)Just a moment|Enable JavaScript and cookies|cf-browser-verification|challenge-platform")

(def ^:private parse-message-pattern
  "Messages from remus/rome and the XML parsers underneath them."
  #"(?i)Invalid document|Non-XML response|Content is not allowed in prolog|entity name must|must be terminated by|must be followed by|Invalid XML")

(defn- header
  "Header lookup that does not care about key case or whether keys are strings."
  [headers header-name]
  (some (fn [[k v]]
          (when (and k (= header-name (string/lower-case (name k)))) v))
        headers))

(defn- challenge?
  "Cloudflare fronts a great many origins, so `server: cloudflare` says nothing about
   who refused the request. `cf-mitigated`, or the interstitial itself, is the signal
   that a bot check rather than the origin turned us away."
  [{:keys [headers body]}]
  (boolean (or (header headers "cf-mitigated")
               (and (string? body) (re-find challenge-body-pattern body)))))

(defn classify-response
  "Classify an HTTP response map, or nil when it is not a failure."
  [{:keys [status] :as response}]
  (when (and status (not (<= 200 status 299)))
    (if (challenge? response)
      :challenge
      :http-status)))

(defn- causes [e]
  (take-while some? (iterate ex-cause e)))

(defn- response-in-chain
  "The HTTP response an exception carries, if any. clj-http, which remus uses,
   puts `:status`/`:headers`/`:body` in the ex-data of the exception it throws."
  [e]
  (some #(let [data (ex-data %)]
           (when (:status data) data))
        (causes e)))

(defn classify-exception
  "Classify a throwable by walking its cause chain. Returns `:unknown` when nothing
   in the chain is recognised, so a caller always has something to dispatch on."
  [e]
  (or (some (fn [c]
              (or (classify-response (ex-data c))
                  (condp instance? c
                    UnknownHostException :dns
                    ; The host resolves and nothing answers, or answers too slowly
                    SocketTimeoutException :timeout
                    ConnectException :timeout
                    NoRouteToHostException :timeout
                    InterruptedIOException :timeout
                    SAXException :parse
                    FeedException :parse
                    (when (some->> (ex-message c) (re-find parse-message-pattern))
                      :parse))))
            (causes e))
      :unknown))

(defn classified?
  "Has this exception already been through `feed-error`?"
  [e]
  (contains? (ex-data e) :super-rss/error))

(defn feed-error
  "Wrap `e` in an exception whose ex-data says what kind of failure this was.
   Pass `response` when the caller holds one the exception does not carry.
   An already-classified exception is returned untouched, so wrapping at both the
   source and the boundary does not nest."
  ([url e] (feed-error url e nil))
  ([url e response]
   (if (classified? e)
     e
     (let [response (or response (response-in-chain e))
           error (or (classify-response response) (classify-exception e))]
       (ex-info (format "Fail to fetch %s (%s): %s" url (name error) (ex-message e))
                (cond-> {:super-rss/error error
                         :url url
                         :cause (ex-message e)}
                  (:status response) (assoc :status (:status response)))
                e)))))

(defn response-error
  "An exception for a response that came back fine at the socket level and is still
   a failure: a non-2xx status, or a bot challenge."
  [url {:keys [status] :as response}]
  (let [error (or (classify-response response) :http-status)]
    (ex-info (format "Fail to fetch %s (%s): status %s" url (name error) status)
             {:super-rss/error error
              :url url
              :status status
              :cause (format "HTTP %s" status)})))
