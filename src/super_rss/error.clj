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
  "Cloudflare's managed-challenge interstitial. The two marker strings are unambiguous;
   the wording is only trusted on a status Cloudflare actually challenges with, so an
   origin 500 whose body happens to say \"just a moment\" stays an origin 500."
  #"(?i)cf-browser-verification|challenge-platform")

(def ^:private challenge-wording-pattern
  #"(?i)Just a moment|Enable JavaScript and cookies")

(def ^:private challenge-statuses
  #{403 429 503})

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
  [{:keys [status headers body]}]
  (boolean (or (header headers "cf-mitigated")
               (and (string? body)
                    (or (re-find challenge-body-pattern body)
                        (and (challenge-statuses status)
                             (re-find challenge-wording-pattern body)))))))

(defn classify-response
  "Classify an HTTP response map, or nil when it is not a failure.
   Total: this sits on a boundary every exception passes through, so a response map
   carrying anything but a numeric status must not turn the real failure into a
   ClassCastException."
  [{:keys [status] :as response}]
  (when (and (number? status) (not (<= 200 status 399)))
    (if (challenge? response)
      :challenge
      :http-status)))

(def ^:private remus-status-pattern
  #"Non-200 status code, status: (\d+)")

(defn- causes [e]
  (take-while some? (iterate ex-cause e)))

(defn- exception-response
  "The HTTP response an exception carries, if any.
   remus raises a non-2xx as a plain RuntimeException with the status in the message
   and nothing in ex-data, so the message is worth reading as well. No headers or body
   come with it, which is why a challenge is only ever recognised where the response
   map itself is in hand."
  [e]
  (let [data (ex-data e)]
    (if (:status data)
      data
      (when-let [[_ status] (some->> (ex-message e) (re-find remus-status-pattern))]
        {:status (parse-long status)}))))

(defn- response-in-chain [e]
  (some exception-response (causes e)))

(defn classify-exception
  "Classify a throwable by walking its cause chain. Returns `:unknown` when nothing
   in the chain is recognised, so a caller always has something to dispatch on."
  [e]
  (or (some (fn [c]
              (or (classify-response (exception-response c))
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
           error (or (classify-response response) (classify-exception e))
           cause (or (ex-message e) (.getName (class e)))]
       (ex-info (format "Fail to fetch %s (%s): %s" url (name error) cause)
                (cond-> {:super-rss/error error
                         :url url
                         :cause cause}
                  (:status response) (assoc :status (:status response)))
                e)))))

(defn response-error
  "An exception for a response that came back fine at the socket level and is still
   a failure: a non-2xx status, or a bot challenge.
   `cause` is the exception that sent us looking, when there was one."
  ([url response] (response-error url response nil))
  ([url {:keys [status] :as response} cause]
   (let [error (or (classify-response response) :http-status)]
     (ex-info (format "Fail to fetch %s (%s): status %s" url (name error) status)
              {:super-rss/error error
               :url url
               :status status
               :cause (format "HTTP %s" status)}
              cause))))

(defn- classify-failures
  "Classify each failure of a cascade, keeping the method that raised it."
  [url failures]
  (map (fn [{:keys [method error]}]
         {:method method :error (feed-error url error)})
       failures))

(defn- primary-failure
  "Strategies run from the most specific to the most hacky, so a later scraper's parse
   error must not mask an earlier :dns or :challenge. The first failure that is not
   :unknown speaks for the run; when nothing is recognised, the first one does."
  [classified]
  (or (first (remove #(= :unknown (:super-rss/error (ex-data (:error %)))) classified))
      (first classified)))

(defn- failure-summary [{:keys [method error]}]
  (-> (ex-data error)
      (select-keys [:super-rss/error :url :status :cause])
      (assoc :method method)))

(defn summarize-failures
  "One line per failed strategy, for the log entry written when a run is swallowed."
  [url failures]
  (->> (classify-failures url failures)
       (map (fn [{:keys [method error]}]
              (format "%s %s" (name method) (name (:super-rss/error (ex-data error))))))
       (string/join ", ")))

(defn cascade-error
  "One exception for a run where every strategy failed.
   `failures` is a seq of `{:method kw :error throwable}` in the order the strategies ran.
   The ex-data is the primary failure's, so `:super-rss/error`, `:url` and `:status` read
   exactly as for a single strategy, plus `:method` naming the strategy that raised it and
   `:errors`, one map per failed strategy in run order. The primary exception is the cause."
  [url failures]
  (let [classified (classify-failures url failures)
        {:keys [method error]} (primary-failure classified)]
    (ex-info (ex-message error)
             (assoc (ex-data error)
                    :method method
                    :errors (mapv failure-summary classified))
             error)))
