(ns super-rss.util-test
  (:require [clojure.test :refer [deftest is]]
            [super-rss.util :as sut]))

(deftest get-base-url-test
  (is (= "https://website.com" (sut/get-base-url "https://website.com/posts"))
      "Base url is ending without '/'"))

(deftest url->absolute-url-test
  (is (= "https://website.com/blog/1" (sut/url->absolute-url "https://website.com/" "/blog/1")))
  (is (= "https://website.com/1" (sut/url->absolute-url "https://website.com/" "/1")))
  (is (= "https://website.com/1" (sut/url->absolute-url "https://website.com/" "1")))
  (is (= "https://website.com/1" (sut/url->absolute-url "https://website.com/blog" "/1")))
  (is (= "https://website.com/blog/1" (sut/url->absolute-url "https://website.com/blog/" "1")))
  (is (= "http://a.com/1" (sut/url->absolute-url "https://website.com/" "http://a.com/1")))
  (is (= "http://company.com/news/inn" (sut/url->absolute-url "http://company.com/news" "/news/inn"))))
