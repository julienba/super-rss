(ns super-rss.impl.common-test
  (:require [clojure.test :refer [is deftest]]
            [super-rss.impl.common :as sut]))

(deftest cleanup-urls-test
  (is (empty? (sut/cleanup-urls "http://a.com/" ["/"]))
      "Not interested in the root url")
  (is (not= ["https://b.com/another"] (sut/cleanup-urls "http://a.com/" ["https://b.com/another"]))
      "Keep external link")
  ;; TODO rollback how this filtering was done before
  (is (= ["http://a.com/blog/article1"
          "http://a.com/blog/article2"
          "http://a.com/blog/article0"]
         (sut/cleanup-urls "http://a.com/" ["http://a.com/blog"
                                            "http://a.com/blog/"
                                            "http://a.com/random"
                                            "http://a.com/random/more"
                                            "http://a.com/blog/article0"
                                            "http://a.com/blog/article1"
                                            "http://a.com/blog/article2"]))
      "Keep links with blog if more than 2")
  (is (= ["http://a.com/random"
          "http://a.com/blog/article1"]
         (sut/cleanup-urls "http://a.com/" ["http://a.com/random"
                                            "http://a.com/blog/article1"]))
      "Not enough URLs with blog"))

