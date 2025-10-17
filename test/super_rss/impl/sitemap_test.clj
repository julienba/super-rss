(ns super-rss.impl.sitemap-test
  (:require [clojure.test :refer [deftest is testing]]
            [super-rss.impl.sitemap :as sut]))

(deftest test-url->title
  (testing "Extract title from URL with hyphens"
    (is (= "Ohpen And Ortec Finance Join Forces To Bring Innovation To Pensions"
           (#'sut/url->title "https://www.ohpen.com/latest-insights/article/ohpen-and-ortec-finance-join-forces-to-bring-innovation-to-pensions"))))

  (testing "Extract title from URL with trailing slash"
    (is (= "Investment Products"
           (#'sut/url->title "https://www.ohpen.com/our-platform/module/investment-products/"))))

  (testing "Extract title from URL with query parameters"
    (is (= "My Article"
           (#'sut/url->title "https://example.com/blog/my-article?utm_source=twitter&id=123"))))

  (testing "Extract title from URL with fragment"
    (is (= "About Us"
           (#'sut/url->title "https://example.com/about-us#team"))))

  (testing "Extract title from URL with .html extension"
    (is (= "Contact"
           (#'sut/url->title "https://example.com/contact.html"))))

  (testing "Extract title from URL with .htm extension"
    (is (= "Index"
           (#'sut/url->title "https://example.com/index.htm"))))

  (testing "Extract title from URL with other extensions"
    (is (= "Document"
           (#'sut/url->title "https://example.com/document.php"))))

  (testing "Extract title from URL with underscores"
    (is (= "My Great Article"
           (#'sut/url->title "https://example.com/my_great_article"))))

  (testing "Extract title from URL with mixed separators"
    (is (= "Some Long Title Here"
           (#'sut/url->title "https://example.com/some-long_title-here"))))

  (testing "Extract title from URL with numbers"
    (is (= "Article 123 Title"
           (#'sut/url->title "https://example.com/article-123-title"))))

  (testing "Extract title from short path"
    (is (= "Leaseplan"
           (#'sut/url->title "https://www.ohpen.com/clients-and-cases/cases/leaseplan"))))

  (testing "Extract title from root URL"
    (is (nil? (#'sut/url->title "https://www.ohpen.com/"))))

  (testing "Extract title from root URL without trailing slash"
    (is (nil? (#'sut/url->title "https://www.ohpen.com"))))

  (testing "Handle nil URL"
    (is (nil? (#'sut/url->title nil))))

  (testing "Handle empty URL"
    (is (nil? (#'sut/url->title ""))))

  (testing "Handle URL with only query params"
    (is (nil? (#'sut/url->title "https://example.com/?page=1"))))

  (testing "Handle URL with special characters in path"
    (is (= "Hello World"
           (#'sut/url->title "https://example.com/hello-world"))))

  (testing "Handle URL with multiple consecutive hyphens"
    (is (= "Multiple Spaces Test"
           (#'sut/url->title "https://example.com/multiple---spaces--test"))))

  (testing "Handle URL with single character segments"
    (is (= "A"
           (#'sut/url->title "https://example.com/path/to/a")))))

(deftest test-cleanup-titles
  (testing "When all titles are nil, generate from URLs"
    (let [input [{:title nil :link "https://example.com/article-one"}
                 {:title nil :link "https://example.com/article-two"}
                 {:title nil :link "https://example.com/article-three"}]
          result (#'sut/cleanup-titles input)]
      (is (= "Article One" (:title (first result))))
      (is (= "Article Two" (:title (second result))))
      (is (= "Article Three" (:title (nth result 2))))))

  (testing "When all titles are the same, generate from URLs"
    (let [input [{:title "Same Title" :link "https://example.com/article-one"}
                 {:title "Same Title" :link "https://example.com/article-two"}
                 {:title "Same Title" :link "https://example.com/article-three"}]
          result (#'sut/cleanup-titles input)]
      (is (= "Article One" (:title (first result))))
      (is (= "Article Two" (:title (second result))))
      (is (= "Article Three" (:title (nth result 2))))))

  (testing "When titles are different, keep original titles"
    (let [input [{:title "First Article" :link "https://example.com/article-one"}
                 {:title "Second Article" :link "https://example.com/article-two"}
                 {:title "Third Article" :link "https://example.com/article-three"}]
          result (#'sut/cleanup-titles input)]
      (is (= "First Article" (:title (first result))))
      (is (= "Second Article" (:title (second result))))
      (is (= "Third Article" (:title (nth result 2))))))

  (testing "Handle empty input"
    (is (nil? (#'sut/cleanup-titles []))))

  (testing "Handle nil input"
    (is (nil? (#'sut/cleanup-titles nil))))

  (testing "Handle single item with nil title"
    (let [input [{:title nil :link "https://example.com/single-article"}]
          result (#'sut/cleanup-titles input)]
      (is (= "Single Article" (:title (first result))))))

  (testing "Handle URL that cannot generate title (root URL)"
    (let [input [{:title nil :link "https://example.com/"}
                 {:title nil :link "https://example.com/article"}]
          result (#'sut/cleanup-titles input)]
      (is (nil? (:title (first result))))
      (is (= "Article" (:title (second result))))))

  (testing "Preserve other fields while updating title"
    (let [input [{:title nil
                  :link "https://example.com/my-article"
                  :description "Some description"
                  :published-date "2025-01-01"}]
          result (#'sut/cleanup-titles input)]
      (is (= "My Article" (:title (first result))))
      (is (= "Some description" (:description (first result))))
      (is (= "2025-01-01" (:published-date (first result))))))

  (testing "Real world example from ohpen.com sitemap"
    (let [input [{:title "Latest Insights"
                  :link "https://www.ohpen.com/latest-insights/article/ohpen-and-ortec-finance-join-forces-to-bring-innovation-to-pensions"}
                 {:title "Latest Insights"
                  :link "https://www.ohpen.com/latest-insights/article/aegon-chooses-ohpen-platform-to-modernise-savings-and-investment-administration"}
                 {:title "Latest Insights"
                  :link "https://www.ohpen.com/latest-insights/article/fintech-company-ohpen-enters-pension-market-through-partnership-with-tkp"}]
          result (#'sut/cleanup-titles input)]
      (is (= "Ohpen And Ortec Finance Join Forces To Bring Innovation To Pensions" (:title (first result))))
      (is (= "Aegon Chooses Ohpen Platform To Modernise Savings And Investment Administration" (:title (second result))))
      (is (= "Fintech Company Ohpen Enters Pension Market Through Partnership With Tkp" (:title (nth result 2)))))))
