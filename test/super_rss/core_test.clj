(ns super-rss.core-test
  "The cascade in `get-feed`: a failing strategy is skipped, the run only raises when
   every strategy failed, and the exception then names the most informative failure."
  (:require [clojure.test :refer [deftest is testing]]
            [super-rss.core :as sut]))

(defn- fetch-stub
  "Build a `fetch` replacement from a map of method -> result.
   A result that is an exception is thrown instead of returned."
  [method->result]
  (fn [method _url _opts]
    (let [result (get method->result method)]
      (if (instance? Throwable result)
        (throw result)
        result))))

(def ^:private two-entries
  {:data [{:link "https://example.com/a" :title "A"}
          {:link "https://example.com/b" :title "B"}]
   :params {:method :smart-links
            :url "https://example.com"}})

(def ^:private sitemap-parse-error
  (ex-info "The entity name must immediately follow the '&' in the entity reference." {}))

(defn- caught [f]
  (try (f) nil (catch Exception e e)))

(deftest get-feed-cascade-test
  (testing "a throwing strategy is skipped and the next one still runs"
    (with-redefs [sut/fetch (fetch-stub {:sitemap sitemap-parse-error
                                         :smart-links two-entries})]
      (let [result (sut/get-feed "https://example.com"
                                 {:method-options [:sitemap :smart-links]}
                                 {})]
        (is (= 2 (count (:results result)))))))

  (testing "with throw? true a later success still wins over an earlier failure"
    ; The langnostic.inaimathi.ca regression from #8: the retained error must not be
    ; thrown once a strategy produced entries.
    (with-redefs [sut/fetch (fetch-stub {:sitemap sitemap-parse-error
                                         :smart-links two-entries})]
      (let [result (sut/get-feed "https://example.com"
                                 {:method-options [:sitemap :smart-links]
                                  :throw? true}
                                 {})]
        (is (= 2 (count (:results result)))))))

  (testing "the cascade stops at the first strategy that produced entries"
    (let [calls (atom [])]
      (with-redefs [sut/fetch (fn [method _url _opts]
                                (swap! calls conj method)
                                (case method
                                  :sitemap (throw sitemap-parse-error)
                                  :smart-links two-entries
                                  :flat-smart-links (throw (ex-info "should not run" {}))))]
        (sut/get-feed "https://example.com"
                      {:method-options [:sitemap :smart-links :flat-smart-links]}
                      {})
        (is (= [:sitemap :smart-links] @calls)))))

  (testing "a strategy returning nothing is skipped too"
    (with-redefs [sut/fetch (fetch-stub {:sitemap {:data [] :params {:method :sitemap}}
                                         :smart-links two-entries})]
      (let [result (sut/get-feed "https://example.com"
                                 {:method-options [:sitemap :smart-links]}
                                 {})]
        (is (= 2 (count (:results result)))))))

  (testing "no exception and no result stays nil even with throw? true"
    (with-redefs [sut/fetch (fetch-stub {})]
      (is (nil? (sut/get-feed "https://example.com"
                              {:method-options [:sitemap :smart-links]
                               :throw? true}
                              {}))))))

(deftest get-feed-every-strategy-fails-test
  (let [all-throw (fetch-stub {:sitemap (ex-info "sitemap boom" {})
                               :smart-links (ex-info "smart-links boom" {})})]
    (testing "with throw? false the failures are swallowed"
      (with-redefs [sut/fetch all-throw]
        (is (nil? (sut/get-feed "https://example.com"
                                {:method-options [:sitemap :smart-links]
                                 :throw? false}
                                {})))))

    (testing "with throw? true one classified exception carries every failure"
      (with-redefs [sut/fetch all-throw]
        (let [e (caught #(sut/get-feed "https://example.com"
                                       {:method-options [:sitemap :smart-links]
                                        :throw? true}
                                       {}))
              data (ex-data e)]
          (is (instance? clojure.lang.ExceptionInfo e))
          (is (= "https://example.com" (:url data)))
          (is (= [{:method :sitemap :super-rss/error :unknown :url "https://example.com" :cause "sitemap boom"}
                  {:method :smart-links :super-rss/error :unknown :url "https://example.com" :cause "smart-links boom"}]
                 (:errors data)))
          (testing "and when nothing is recognised the first failure speaks for the run"
            (is (= :unknown (:super-rss/error data)))
            (is (= :sitemap (:method data)))
            (is (= "sitemap boom" (:cause data)))
            ; cascade -> classified -> the exception the strategy actually threw
            (is (= "sitemap boom" (ex-message (ex-cause (ex-cause e)))))))))))

(deftest get-feed-primary-failure-test
  (testing "an earlier recognised failure is not masked by a later scraper blow-up"
    ; Default order runs the specific strategies first; the scrapers that follow fail
    ; on the same dead host with less informative errors.
    (with-redefs [sut/fetch (fetch-stub {:find-rss-url (java.net.UnknownHostException. "example.com")
                                         :sitemap (ex-info "sitemap boom" {})
                                         :smart-links (ex-info "The href cannot be found in the document" {})})]
      (let [e (caught #(sut/get-feed "https://example.com"
                                     {:method-options [:find-rss-url :sitemap :smart-links]
                                      :throw? true}
                                     {}))
            data (ex-data e)]
        (is (= :dns (:super-rss/error data)))
        (is (= :find-rss-url (:method data)))
        (is (= [:dns :unknown :unknown] (map :super-rss/error (:errors data)))))))

  (testing "a recognised failure later in the run beats an unrecognised earlier one"
    (with-redefs [sut/fetch (fetch-stub {:sitemap (ex-info "sitemap boom" {})
                                         :smart-links (ex-info "Invalid document" {})})]
      (let [data (ex-data (caught #(sut/get-feed "https://example.com"
                                                 {:method-options [:sitemap :smart-links]
                                                  :throw? true}
                                                 {})))]
        (is (= :parse (:super-rss/error data)))
        (is (= :smart-links (:method data))))))

  (testing "an exception already classified by a strategy keeps its own ex-data"
    (let [classified (ex-info "Fail to fetch https://example.com/feed (challenge): status 403"
                              {:super-rss/error :challenge
                               :url "https://example.com/feed"
                               :status 403
                               :cause "HTTP 403"})]
      (with-redefs [sut/fetch (fetch-stub {:find-rss-url classified
                                           :sitemap (ex-info "sitemap boom" {})})]
        (let [data (ex-data (caught #(sut/get-feed "https://example.com"
                                                   {:method-options [:find-rss-url :sitemap]
                                                    :throw? true}
                                                   {})))]
          (is (= :challenge (:super-rss/error data)))
          (is (= 403 (:status data)))
          (is (= "https://example.com/feed" (:url data))))))))

(deftest get-feed-single-method-test
  (testing "an explicit single method follows the same throw? semantics"
    (with-redefs [sut/fetch (fetch-stub {:sitemap (ex-info "sitemap boom" {})})]
      (is (nil? (sut/get-feed "https://example.com" {:method :sitemap :throw? false} {})))
      (let [data (ex-data (caught #(sut/get-feed "https://example.com" {:method :sitemap :throw? true} {})))]
        (is (= :sitemap (:method data)))
        (is (= 1 (count (:errors data))))))))
