(ns super-rss.http-test
  (:require [clojure.test :refer [deftest is testing]]
            exoscale.ok-http
            [super-rss.http :as sut]))

(deftest headers-test
  (testing "the default User-Agent identifies the library and carries a contact URL"
    (is (= sut/default-user-agent (get (sut/headers nil) "User-Agent")))
    (is (= sut/default-user-agent (get (sut/headers nil {}) "User-Agent")))
    (is (re-find #"\(\+https://" sut/default-user-agent)))

  (testing "the per-call context is kept as a breadcrumb suffix"
    (is (= (str sut/default-user-agent " sitemap-reader")
           (get (sut/headers "sitemap-reader") "User-Agent"))))

  (testing "a configured user-agent replaces the default and keeps the breadcrumb"
    (is (= "my-reader/2.0 (+https://example.com) sitemap-reader"
           (get (sut/headers "sitemap-reader" {:user-agent "my-reader/2.0 (+https://example.com)"})
                "User-Agent"))))

  (testing "every call asks for a feed"
    (is (= sut/accept (get (sut/headers "rss-reader") "Accept")))
    (is (re-find #"application/rss\+xml" sut/accept))
    (is (re-find #"application/atom\+xml" sut/accept))))

(deftest request-default-headers-test
  (let [captured (atom nil)]
    (with-redefs [exoscale.ok-http/request (fn [_client req] (reset! captured req) {:status 200 :body ""})]
      (testing "a call that sets no headers of its own still identifies itself"
        (sut/get "https://example.com/feed")
        (is (= sut/default-user-agent (get-in @captured [:headers "User-Agent"])))
        (is (= sut/accept (get-in @captured [:headers "Accept"]))))

      (testing "per-call headers are merged on top of the defaults, not replacing them"
        (sut/get "https://example.com/feed" {:headers (sut/headers "rss-reader" {:user-agent "my-reader/2.0"})})
        (is (= "my-reader/2.0 rss-reader" (get-in @captured [:headers "User-Agent"])))
        (is (= sut/accept (get-in @captured [:headers "Accept"]))))

      (testing "an unrelated header does not drop the defaults"
        (sut/get "https://example.com/feed" {:headers {"If-None-Match" "abc"}})
        (is (= "abc" (get-in @captured [:headers "If-None-Match"])))
        (is (= sut/default-user-agent (get-in @captured [:headers "User-Agent"])))))))
