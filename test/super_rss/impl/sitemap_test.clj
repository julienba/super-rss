(ns super-rss.impl.sitemap-test
  (:require [clojure.instant :as instant]
            [clojure.string :as string]
            [clojure.test :refer [are deftest is testing]]
            [super-rss.html]
            [super-rss.http]
            [super-rss.impl.common :as common]
            [super-rss.impl.sitemap :as sut]
            [super-rss.robots-txt]))

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

(deftest test-find-sitemap-url-in-robots
  (testing "Single sitemap in robots.txt with Sitemap key"
    (is (= "https://example.com/sitemap.xml"
           (#'sut/find-sitemap-url-in-robots-contents "https://example.com"
                                                      [{:key "User-agent" :value "*"}
                                                       {:key "Sitemap" :value "https://example.com/sitemap.xml"}]))))

  (testing "Single sitemap in robots.txt with lowercase sitemap key"
    (is (= "https://example.com/sitemap.xml"
           (#'sut/find-sitemap-url-in-robots-contents "https://example.com"
                                                      [{:key "User-agent" :value "*"}
                                                       {:key "sitemap" :value "https://example.com/sitemap.xml"}]))))

  (testing "Multiple sitemaps - returns first when no article prefix match"
    (is (= "https://example.com/sitemap.xml"
           (#'sut/find-sitemap-url-in-robots-contents "https://example.com"
                                                      [{:key "User-agent" :value "*"}
                                                       {:key "sitemap" :value "https://example.com/sitemap.xml"}
                                                       {:key "sitemap" :value "https://example.com/other-sitemap.xml"}
                                                       {:key "sitemap" :value "https://example.com/third-sitemap.xml"}]))))

  (testing "Multiple sitemaps with article prefix match - real world rasa.com case"
    (is "https://rasa.com/blog/sitemap.xml"
        (#'sut/find-sitemap-url-in-robots-contents "https://rasa.com"
                                                   [{:key "User-agent" :value "*"}
                                                    {:key "sitemap" :value "https://rasa.com/sitemap.xml"}
                                                    {:key "sitemap" :value "https://rasa.com/blog/sitemap.xml"}
                                                    {:key "sitemap" :value "https://rasa.com/summit/sitemap.xml"}
                                                    {:key "sitemap" :value "https://rasa.com/docs/rasa/sitemap.xml"}
                                                    {:key "sitemap" :value "https://rasa.com/docs/rasa-x/sitemap.xml"}
                                                    {:key "sitemap" :value "https://rasa.community/sitemap.xml"}])))

  (testing "No sitemap in robots.txt - returns empty sequence or nil"
    (is (nil? (#'sut/find-sitemap-url-in-robots-contents "https://example.com"
                                                         [{:key "User-agent" :value "*"}
                                                          {:key "Disallow" :value "/admin/"}]))))
  (testing "robots.txt can have comment"
    (is (= "https://dust.tt/sitemap.xml"
           (#'sut/find-sitemap-url-in-robots-contents "https://dust.tt"
                                                      [{:key "# *", :value nil}
                                                       {:key "User-agent", :value "*"}
                                                       {:key "Allow", :value "/"}
                                                       {:key "Disallow", :value "/api*"}
                                                       {:key "Disallow", :value "/w*"}
                                                       {:key "Disallow", :value "/poke*"}
                                                       {:key "Disallow", :value "/oauth*"}
                                                       {:key "Disallow", :value "/sso-enforced"}
                                                       {:key "Disallow", :value "/no-workspace"}
                                                       {:key "Disallow", :value "/maintenance"}
                                                       {:key "Disallow", :value "/login-error"}
                                                       {:key "# Host", :value nil}
                                                       {:key "Host", :value "https://dust.tt"}
                                                       {:key "# Sitemaps", :value nil}
                                                       {:key "Sitemap", :value "https://dust.tt/sitemap.xml"}])))))

(defn- parse-sitemap-entry
  "Extract URL and lastmod from a sitemap entry."
  [{:keys [content]}]
  (let [url (->> content
                 (filter #(= :loc (:tag %)))
                 first
                 :content
                 first)
        lastmod (->> content
                     (filter #(= :lastmod (:tag %)))
                     first
                     :content
                     first)]
    {:url url
     :lastmod (when lastmod
                (try
                  (instant/read-instant-date lastmod)
                  (catch Exception _)))}))

(defn- has-article-prefix?
  "Check if a URL (without base URL) matches the article prefix pattern."
  [base-url url-entry]
  (when-let [url (:url url-entry)]
    (re-find common/article-prefix (string/replace-first url base-url ""))))

(defn- is-blog-url?
  "Check if a URL is a valid blog URL."
  [url-entry]
  (when-let [url (:url url-entry)]
    (common/blog-url? url)))

(defn- non-nil-url?
  "Check if URL entry has a non-nil URL."
  [url-entry]
  (some? (:url url-entry)))

(def mock-insights-sitemap-xml
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
  <urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">
    <url>
      <loc>https://gamma.app/insights/</loc>
      <lastmod>2024-01-15T10:00:00Z</lastmod>
    </url>
    <url>
      <loc>https://gamma.app/insights/ai-powered-presentations</loc>
      <lastmod>2024-01-20T10:00:00Z</lastmod>
    </url>
    <url>
      <loc>https://gamma.app/insights/future-of-work</loc>
      <lastmod>2024-01-25T10:00:00Z</lastmod>
    </url>
    <url>
      <loc>https://gamma.app/insights/productivity-tips</loc>
      <lastmod>2024-01-30T10:00:00Z</lastmod>
    </url>
    <url>
      <loc>https://gamma.app/about</loc>
      <lastmod>2024-01-10T10:00:00Z</lastmod>
    </url>
    <url>
      <loc>https://gamma.app/contact</loc>
      <lastmod>2024-01-10T10:00:00Z</lastmod>
    </url>
    <url>
      <loc>https://gamma.app/insights/2</loc>
      <lastmod>2024-01-15T10:00:00Z</lastmod>
    </url>
  </urlset>")

(deftest test-sitemap-with-insights-urls
  (testing "Sitemap with /insights/ URLs should be recognized and filtered correctly"
    (let [base-url "https://gamma.app"
          parsed-sitemap (#'sut/parse-xml-string mock-insights-sitemap-xml)
          content-list (:content parsed-sitemap)
          url-list (map parse-sitemap-entry content-list)]

      (is (common/blog-url? "https://gamma.app/insights/ai-powered-presentations")
          "/insights/ URLs should be recognized as blog URLs")
      (is (not (common/blog-url? "https://gamma.app/insights/"))
          "/insights/ root should not be recognized as blog URL")
      (is (not (common/blog-url? "https://gamma.app/insights/2"))
          "/insights/ pagination should not be recognized as blog URL")

      (is (re-find common/article-prefix "/insights/ai-powered-presentations")
          "article-prefix should match /insights URLs")

      (let [sitemap-contents-filter (->> url-list
                                         (filter non-nil-url?)
                                         (filter #(has-article-prefix? base-url %)))
            urls-prefixed? (< 2 (count sitemap-contents-filter))
            clean-sitemap-contents (if urls-prefixed?
                                     sitemap-contents-filter
                                     url-list)
            clean-sitemap-contents (->> clean-sitemap-contents
                                        (remove nil?)
                                        (filter non-nil-url?)
                                        (filter is-blog-url?))]
        (is (some #(= (:url %) "https://gamma.app/insights/ai-powered-presentations") clean-sitemap-contents)
            "Should include /insights/ article URLs")
        (is (some #(= (:url %) "https://gamma.app/insights/future-of-work") clean-sitemap-contents)
            "Should include /insights/ article URLs")
        (is (some #(= (:url %) "https://gamma.app/insights/productivity-tips") clean-sitemap-contents)
            "Should include /insights/ article URLs")
        (is (not-any? #(= (:url %) "https://gamma.app/insights/") clean-sitemap-contents)
            "Should exclude /insights/ root URL")
        (is (not-any? #(= (:url %) "https://gamma.app/insights/2") clean-sitemap-contents)
            "Should exclude /insights/ pagination URLs")
        (is (not-any? #(= (:url %) "https://gamma.app/about") clean-sitemap-contents)
            "Should exclude non-article URLs")
        (is (not-any? #(= (:url %) "https://gamma.app/contact") clean-sitemap-contents)
            "Should exclude non-article URLs")))))


(def ^:private wp-rocket-html
  "<!doctype html><html><head>
   <link rel=\"preload\" href=\"/style.css\" data-rocket-prefetch as=\"style\">
   </head><body><h1>Page not found</h1></body></html>")

(def ^:private bare-ampersand-html
  "<!doctype html><html><body><p>Tips & tricks</p></body></html>")

(def ^:private valid-sitemap
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
   <urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">
     <url><loc>https://example.com/blog/first</loc><lastmod>2026-01-02</lastmod></url>
     <url><loc>https://example.com/blog/second</loc><lastmod>2026-03-04</lastmod></url>
   </urlset>")

(defn- responding
  "An `http/get` stub answering every URL with the given status/body."
  [status body]
  (fn [_url & _opts] {:status status :body body}))

(deftest fetch-sitemap-non-xml-test
  (testing "HTML served at a sitemap path yields nothing instead of throwing"
    (are [body description] (nil? (with-redefs [super-rss.http/get (responding 200 body)]
                                    (#'sut/fetch-sitemap "https://example.com/sitemap.xml")))
      wp-rocket-html "WP Rocket page with an unquoted data-rocket-prefetch attribute"
      bare-ampersand-html "HTML page containing a bare &"
      "<!doctype html><html><head><meta charset=\"utf-8\"></head></html>" "HTML with an unterminated <meta>"
      "" "empty body"
      "Not found" "plain text soft-404"))

  (testing "a body that opens like XML but is malformed yields nothing instead of throwing"
    (is (nil? (with-redefs [super-rss.http/get (responding 200 "<?xml version=\"1.0\"?><urlset><url><loc>oops")]
                (#'sut/fetch-sitemap "https://example.com/sitemap.xml")))))

  (testing "a well-formed sitemap still parses, newest first"
    (let [result (with-redefs [super-rss.http/get (responding 200 valid-sitemap)]
                   (#'sut/fetch-sitemap "https://example.com/sitemap.xml"))]
      (is (= ["https://example.com/blog/second" "https://example.com/blog/first"]
             (map :url result)))
      (is (= (instant/read-instant-date "2026-03-04") (:lastmod (first result))))))

  (testing "a non-200 yields nothing"
    (is (nil? (with-redefs [super-rss.http/get (responding 404 valid-sitemap)]
                (#'sut/fetch-sitemap "https://example.com/sitemap.xml"))))))

(deftest find-sitemap-url-fallback-test
  (testing "the /sitemap.xml fallback is not accepted when the body is not XML"
    (with-redefs [super-rss.robots-txt/get-robots-txt (constantly nil)
                  super-rss.html/fetch (constantly nil)
                  super-rss.http/get (responding 200 wp-rocket-html)]
      (is (nil? (#'sut/find-sitemap-url "https://example.com")))))

  (testing "the /sitemap.xml fallback is accepted when the body is XML"
    (with-redefs [super-rss.robots-txt/get-robots-txt (constantly nil)
                  super-rss.html/fetch (constantly nil)
                  super-rss.http/get (responding 200 valid-sitemap)]
      (is (= "https://example.com/sitemap.xml"
             (#'sut/find-sitemap-url "https://example.com"))))))
