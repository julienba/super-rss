(ns super-rss.util-test
  (:require [clojure.test :refer [deftest testing is]]
            [super-rss.util :as sut]))

(deftest miscs-test
  (testing "Base url is ending without '/'"
    (is (= "https://website.com" (sut/get-base-url "https://website.com/posts")))))
