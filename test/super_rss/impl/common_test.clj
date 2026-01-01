(ns super-rss.impl.common-test
  (:require [clojure.test :refer [is deftest]]
            [super-rss.impl.common :as sut]))

(deftest cleanup-urls-test
  (is (empty? (sut/cleanup-urls "http://a.com/" ["/"]))
      "Not interested in the root url")
  (is (not= ["https://b.com/another"] (sut/cleanup-urls "http://a.com/" ["https://b.com/another"]))
      "Keep external link")
  (is (= ["http://a.com/blog/article0"
          "http://a.com/blog/article1"
          "http://a.com/blog/article2"]
         (sort (sut/cleanup-urls "http://a.com/" ["http://a.com/blog"
                                                  "http://a.com/blog/"
                                                  "http://a.com/random"
                                                  "http://a.com/random/more"
                                                  "http://a.com/blog/article0"
                                                  "http://a.com/blog/article1"
                                                  "http://a.com/blog/article2"])))
      "Keep links with blog if more than 2")
  (is (= ["http://a.com/random"
          "http://a.com/blog/article1"]
         (sut/cleanup-urls "http://a.com/" ["http://a.com/random"
                                            "http://a.com/blog/article1"]))
      "Not enough URLs with blog")
  (is (= ["http://a.com/insights/article0"
          "http://a.com/insights/article1"
          "http://a.com/insights/article2"]
         (sort (sut/cleanup-urls "http://a.com/" ["http://a.com/insights"
                                                  "http://a.com/insights/"
                                                  "http://a.com/random"
                                                  "http://a.com/random/more"
                                                  "http://a.com/insights/article0"
                                                  "http://a.com/insights/article1"
                                                  "http://a.com/insights/article2"])))
      "Keep links with insights if more than 2")
  (is (empty? (sut/cleanup-urls "http://a.com/" ["http://a.com/insights"
                                                 "http://a.com/insights/"]))
      "Filter out insights root URLs")
  (is (empty? (sut/cleanup-urls "http://a.com/" ["http://a.com/insights/2"
                                                 "http://a.com/insights/3"]))
      "Filter out insights pagination URLs"))

(deftest blog-url?-test
  (is (sut/blog-url? "http://example.com/blog/my-post")
      "Standard /blog/ prefix")
  (is (sut/blog-url? "http://example.com/article/my-post")
      "Singular /article/ prefix")
  (is (sut/blog-url? "http://example.com/articles/my-post")
      "Plural /articles/ prefix - helionenergy.com uses this")
  (is (sut/blog-url? "http://example.com/news/my-post")
      "/news/ prefix")
  (is (sut/blog-url? "http://example.com/insights/my-post")
      "/insights/ prefix")
  (is (sut/blog-url? "http://example.com/library/my-post")
      "/library/ prefix - design.google uses this")
  (is (sut/blog-url? "http://example.com/post/my-post")
      "/post/ prefix")
  (is (not (sut/blog-url? "http://example.com/about"))
      "Non-blog URLs should not match")
  (is (not (sut/blog-url? "http://example.com/blog/"))
      "Blog root should not match")
  (is (not (sut/blog-url? "http://example.com/news/"))
      "News root should not match")
  (is (not (sut/blog-url? "http://example.com/blog/category/tech"))
      "Category pages should not match")
  (is (not (sut/blog-url? "http://example.com/blog/author/john"))
      "Author pages should not match"))

