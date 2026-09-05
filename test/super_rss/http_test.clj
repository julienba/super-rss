(ns super-rss.http-test
  (:require [clojure.test :refer [deftest is testing]]
            exoscale.ok-http
            [super-rss.http :as sut]))

(deftest headers-test
  (testing "the default User-Agent identifies the library and carries a contact URL"
    (is (= sut/default-user-agent (get (sut/headers nil nil) "User-Agent")))
    (is (= sut/default-user-agent (get (sut/headers nil nil {}) "User-Agent")))
    (is (re-find #"\(\+https://" sut/default-user-agent)))

  (testing "the per-call context is kept as a breadcrumb suffix"
    (is (= (str sut/default-user-agent " sitemap-reader")
           (get (sut/headers "sitemap-reader" nil) "User-Agent"))))

  (testing "a configured user-agent leads, and the super-rss identity stays in the string"
    (let [ua (get (sut/headers "sitemap-reader" nil {:user-agent "my-reader/2.0 (+https://example.com)"})
                  "User-Agent")]
      (is (= "my-reader/2.0 (+https://example.com) super-rss (+https://github.com/julienba/super-rss) sitemap-reader" ua))
      (is (re-find #"super-rss" ua)
          "a site operator seeing the breadcrumb can tell what it is")))

  (testing "each call asks for the representation it actually parses"
    (is (= sut/feed-accept (get (sut/headers "rss-reader" sut/feed-accept) "Accept")))
    (is (= sut/html-accept (get (sut/headers "poor-man-rss" sut/html-accept) "Accept")))
    (is (re-find #"application/rss\+xml" sut/feed-accept))
    (is (re-find #"text/html" sut/html-accept)))

  (testing "no Accept is sent when the call has no preference"
    (is (not (contains? (sut/headers "robots-txt-reader" nil) "Accept")))))

(deftest request-default-headers-test
  (let [captured (atom nil)]
    (with-redefs [exoscale.ok-http/request (fn [_client req] (reset! captured req) {:status 200 :body ""})]
      (testing "a call that sets no headers of its own still identifies itself"
        (sut/get "https://example.com/feed")
        (is (= sut/default-user-agent (get-in @captured [:headers "User-Agent"]))))

      (testing "no Accept is imposed on a call that did not ask for one"
        (is (not (contains? (:headers @captured) "Accept"))
            "a feed-preferring Accept on an HTML fetch could flip the response to a feed"))

      (testing "per-call headers are merged on top of the defaults, not replacing them"
        (sut/get "https://example.com/feed" {:headers (sut/headers "rss-reader" sut/feed-accept {:user-agent "my-reader/2.0"})})
        (is (re-find #"^my-reader/2\.0 super-rss .* rss-reader$" (get-in @captured [:headers "User-Agent"])))
        (is (= sut/feed-accept (get-in @captured [:headers "Accept"]))))

      (testing "an unrelated header does not drop the identity"
        (sut/get "https://example.com/feed" {:headers {"If-None-Match" "abc"}})
        (is (= "abc" (get-in @captured [:headers "If-None-Match"])))
        (is (= sut/default-user-agent (get-in @captured [:headers "User-Agent"])))))))
