(ns super-rss.impl.common-test
  (:require [clojure.test :refer [is deftest]]
            [super-rss.impl.common :as sut]))

(deftest cleanup-urls-test
  (is (empty? (sut/cleanup-urls "http://a.com/" ["/"]))
      "Not interested in the root url")
  (is (= ["https://b.com/another"] (sut/cleanup-urls "http://a.com/" ["https://b.com/another"]))
      "Keep external link")
  (is (= ["http://a.com/blog/article1"
          "http://a.com/blog/article2"
          "http://a.com/blog/article0"]
         (sut/cleanup-urls "http://a.com/" ["http://a.com/blog"
                                            "http://a.com/blog/"
                                            "http://a.com/random"
                                            "http://a.com/random/more"
                                            "http://a.com/blog/article1"
                                            "http://a.com/blog/article2"
                                            "/blog"
                                            "/blog/"
                                            "/random"
                                            "/random/more"
                                            "/blog/article1"
                                            "/blog/article2"
                                            "/"
                                            "blog/article0"]))
      "Keep links with blog if more than 2")
  (is (= ["http://a.com/random"
          "http://a.com/blog/article1"]
         (sut/cleanup-urls "http://a.com/" ["http://a.com/random"
                                            "http://a.com/blog/article1"]))
      "Not enough URL with blog"))

