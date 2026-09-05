(ns super-rss.error-test
  (:require [clojure.test :refer [are deftest is testing]]
            [super-rss.error :as sut])
  (:import [com.rometools.rome.io ParsingFeedException]
           [java.net ConnectException SocketTimeoutException UnknownHostException]
           [org.xml.sax SAXParseException]))

;; One saved response or exception instance per row of the table in #12,
;; so the classifier is pinned down without going over the network.

(def ^:private cloudflare-challenge
  {:status 403
   :headers {"server" "cloudflare"
             "cf-mitigated" "challenge"
             "content-type" "text/html; charset=UTF-8"}
   :body "<!DOCTYPE html><html><head><title>Just a moment...</title></head><body>Enable JavaScript and cookies to continue</body></html>"})

(def ^:private origin-403
  {:status 403
   :headers {"server" "cloudflare"
             "content-type" "text/html"}
   :body "<html><head><title>Error 403</title></head><body>Forbidden</body></html>"})

(def ^:private origin-504
  {:status 504
   :headers {"server" "cloudflare"}
   :body "<html><head><title>Gateway time-out</title></head><body>Error 504</body></html>"})

(deftest classify-response-test
  (testing "a bot challenge is not the same failure as the origin refusing"
    (is (= :challenge (sut/classify-response cloudflare-challenge))
        "cf-mitigated marks a managed challenge (developerblog.zendesk.com)")
    (is (= :http-status (sut/classify-response origin-403))
        "a WordPress VIP origin error behind Cloudflare is not a challenge (doordash.engineering)")
    (is (= :http-status (sut/classify-response origin-504))
        "Cloudflare reporting an origin timeout is not a challenge (technology.riotgames.com)"))

  (testing "the challenge body alone is enough when the header is absent"
    (is (= :challenge (sut/classify-response {:status 403
                                              :headers {}
                                              :body "<title>Just a moment...</title>"}))))

  (testing "header case and keyword keys do not matter"
    (is (= :challenge (sut/classify-response {:status 403 :headers {"CF-Mitigated" "challenge"}})))
    (is (= :challenge (sut/classify-response {:status 403 :headers {:cf-mitigated "challenge"}}))))

  (testing "a successful response is not a failure"
    (are [status] (nil? (sut/classify-response {:status status :headers {} :body "ok"}))
      200 201 204))

  (testing "no status is not a failure this classifier can speak to"
    (is (nil? (sut/classify-response {})))
    (is (nil? (sut/classify-response nil)))))

(deftest classify-exception-test
  (testing "DNS"
    (is (= :dns (sut/classify-exception (UnknownHostException. "blog.alpharun.com")))
        "NXDOMAIN")
    (is (= :dns (sut/classify-exception (ex-info "wrapped" {} (UnknownHostException. "blog.sqreen.com"))))
        "SERVFAIL, reached through a cause chain"))

  (testing "timeout"
    (is (= :timeout (sut/classify-exception (SocketTimeoutException. "connect timed out"))))
    (is (= :timeout (sut/classify-exception (ConnectException. "Connection timed out")))
        "host resolves, nothing listens (buildingvts.com)"))

  (testing "HTTP status carried in ex-data, the shape clj-http throws"
    (is (= :challenge (sut/classify-exception (ex-info "clj-http: status 403" cloudflare-challenge))))
    (is (= :http-status (sut/classify-exception (ex-info "clj-http: status 403" origin-403))))
    (is (= :http-status (sut/classify-exception (ex-info "clj-http: status 504" origin-504)))))

  (testing "parse"
    (is (= :parse (sut/classify-exception (SAXParseException. "The entity name must immediately follow the '&' in the entity reference." nil))))
    (is (= :parse (sut/classify-exception (ParsingFeedException. "Invalid document"))))
    (is (= :parse (sut/classify-exception (RuntimeException. "Invalid document")))
        "remus reports an xmlns-less <feed> as a plain RuntimeException (langnostic.inaimathi.ca)")
    (is (= :parse (sut/classify-exception (RuntimeException. "Non-XML response, status: 200")))))

  (testing "anything unrecognised still gives the caller something to dispatch on"
    (is (= :unknown (sut/classify-exception (RuntimeException. "something else entirely"))))
    (is (= :unknown (sut/classify-exception (RuntimeException.))))))

(deftest feed-error-test
  (let [url "https://example.com/feed"]
    (testing "ex-data carries the class, the url and the cause"
      (let [e (sut/feed-error url (UnknownHostException. "example.com"))]
        (is (= {:super-rss/error :dns :url url :cause "example.com"} (ex-data e)))
        (is (instance? UnknownHostException (ex-cause e))
            "the original exception is kept as the cause")))

    (testing "status is attached when the failure came with a response"
      (is (= {:super-rss/error :challenge :url url :status 403 :cause "status 403"}
             (ex-data (sut/feed-error url (ex-info "status 403" cloudflare-challenge))))))

    (testing "a response passed by the caller is used when the exception carries none"
      (is (= :http-status (:super-rss/error (ex-data (sut/feed-error url (RuntimeException. "boom") origin-504)))))
      (is (= 504 (:status (ex-data (sut/feed-error url (RuntimeException. "boom") origin-504))))))

    (testing "wrapping at both the source and the boundary does not nest"
      (let [once (sut/feed-error url (UnknownHostException. "example.com"))]
        (is (identical? once (sut/feed-error url once)))))

    (testing "response-error describes a failure that never threw"
      (is (= {:super-rss/error :challenge :url url :status 403 :cause "HTTP 403"}
             (ex-data (sut/response-error url cloudflare-challenge))))
      (is (= :http-status (:super-rss/error (ex-data (sut/response-error url origin-403))))))))
