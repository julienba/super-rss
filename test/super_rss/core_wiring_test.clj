(ns super-rss.core-wiring-test
  "The classifier is unit-tested in error-test; these pin the wiring, which is where
   a classified exception is easiest to lose."
  (:require [clojure.test :refer [deftest is testing]]
            [remus]
            [super-rss.core :as sut]
            [super-rss.http]))

(def ^:private challenge-response
  {:status 403
   :headers {"server" "cloudflare"
             "cf-mitigated" "challenge"
             "content-type" "text/html; charset=UTF-8"}
   :body "<!DOCTYPE html><html><head><title>Just a moment...</title></head></html>"})

(def ^:private feed-link-page
  {:status 200
   :headers {"content-type" "text/html"}
   :body "<html><head><link rel=\"alternate\" type=\"application/rss+xml\" href=\"/feed\"></head><body></body></html>"})

(defn- serving
  "Answer the page with a <link> to the feed, and the feed itself with `feed-response`."
  [feed-response]
  (fn [url & _]
    (if (re-find #"/feed" url)
      feed-response
      feed-link-page)))

(deftest challenge-is-classified-through-get-feed-test
  (testing "a bot challenge found while validating a discovered feed URL"
    (with-redefs [super-rss.http/get (serving challenge-response)]
      (testing "reaches the caller classified with :throw? true"
        (let [e (try (sut/get-feed "https://example.com" {:method-options [:find-rss-url] :throw? true} {})
                     (catch Exception e e))]
          (is (instance? Exception e))
          (is (= :challenge (:super-rss/error (ex-data e))))
          (is (= 403 (:status (ex-data e))))
          (is (= "https://example.com/feed" (:url (ex-data e))))))

      (testing "is still swallowed with the default :throw? false"
        (is (nil? (sut/get-feed "https://example.com" {:method-options [:find-rss-url]} {})))))))

(deftest direct-rss-status-is-classified-test
  (testing "the non-2xx remus reports in a bare RuntimeException message survives as :http-status"
    (with-redefs [remus/parse-url (fn [& _]
                                    (throw (RuntimeException.
                                            "Non-200 status code, status: 504, url: https://example.com/feed, content-type: text/html")))]
      (let [e (try (sut/get-feed "https://example.com/feed" {:method :direct-rss :throw? true} {})
                   (catch Exception e e))]
        (is (= :http-status (:super-rss/error (ex-data e))))
        (is (= 504 (:status (ex-data e))))))))

(deftest dns-is-classified-test
  (testing "an unresolvable host reaches the caller as :dns"
    (with-redefs [remus/parse-url (fn [& _] (throw (java.net.UnknownHostException. "blog.alpharun.com")))]
      (let [e (try (sut/get-feed "https://blog.alpharun.com/feed" {:method :direct-rss :throw? true} {})
                   (catch Exception e e))]
        (is (= :dns (:super-rss/error (ex-data e))))))))
