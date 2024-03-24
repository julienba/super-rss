(ns super-rss.impl.smart-links-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [is deftest testing]]
            [super-rss.impl.smart-links :as sut]
            [super-rss.html :as rss.html]))

(defn load-html [file-path]
  (-> (slurp (io/resource file-path))
      (#'rss.html/html->hickory)))

(def sample1 (load-html "smart_links/sample1.html"))
(def sample2 (load-html "smart_links/sample2.html"))
(def sample3 (load-html "smart_links/sample3.html"))

(deftest find-all-links-test
  (is (= ["http://company.com/framework/"
          "http://company.com/about-us/"
          "http://company.com/contact/"
          "http://company.com/company-selected-as-a-venture-atlanta-2023-presenting-company/"
          "http://company.com/company-expands-into-tiruchirappalli-india/"
          "http://company.com/writing-a-simple-mcumgr-client-with-rust/"
          "http://company.com/finding-our-rhythm-a-creative-approach-to-documentation-woes/"
          "http://company.com/company-io-recognized-as-one-of-atlantas-fastest-growing-companies-at-the-2022-atlanta-pacesetter-awards/"
          "http://company.com/a-simple-tool-for-load-testing-stateful-systems-using-clojure/"
          "http://company.com/the-value-of-principles/"
          "http://company.com/how-we-use-react-context-to-customize-our-apps-for-our-clients/"
          "http://company.com/develop-a-digital-key-app-using-company-sdk/"
          "http://company.com/developing-mobile-digital-key-applications-with-clojurescript/"
          "http://company.com/return-of-the-fob/"
          "http://company.com/reversing-sherlock/"
          "http://company.com/betting-the-company-on-clojurescript/"
          "http://company.com/is-1-password-still-1-password-too-many/"]
         (#'sut/find-all-links "http://company.com/" sample1))
      "Links are found")
  (is (= ["http://company.com/blog/framework/"
          "http://company.com/blog/about-us/"
          "http://company.com/blog/contact/"
          "http://company.com/blog/company-selected-as-a-venture-atlanta-2023-presenting-company/"
          "http://company.com/blog/company-expands-into-tiruchirappalli-india/"
          "http://company.com/blog/writing-a-simple-mcumgr-client-with-rust/"
          "http://company.com/blog/finding-our-rhythm-a-creative-approach-to-documentation-woes/"
          "http://company.com/blog/company-io-recognized-as-one-of-atlantas-fastest-growing-companies-at-the-2022-atlanta-pacesetter-awards/"
          "http://company.com/blog/a-simple-tool-for-load-testing-stateful-systems-using-clojure/"
          "http://company.com/blog/the-value-of-principles/"
          "http://company.com/blog/how-we-use-react-context-to-customize-our-apps-for-our-clients/"
          "http://company.com/blog/develop-a-digital-key-app-using-company-sdk/"
          "http://company.com/blog/developing-mobile-digital-key-applications-with-clojurescript/"
          "http://company.com/blog/return-of-the-fob/"
          "http://company.com/blog/reversing-sherlock/"
          "http://company.com/blog/betting-the-company-on-clojurescript/"
          "http://company.com/blog/is-1-password-still-1-password-too-many/"]
         (#'sut/find-all-links "http://company.com/blog" sample1))
      "Root url is preserved in each relative URL")
  (is (= ["http://company.com/2024-03-15-america-running-out-Elec-big-tech-demand-power.html"
          "http://company.com/author/avagrace"
          "http://company.com/2024-03-01-biden-green-tax-credit-plan-gift-china.html"
          "http://company.com/2024-02-19-environmentally-unfriendly-ev-roads-strain-infrastructure.html"
          "http://company.com/author/ethanh"
          "http://company.com/2024-02-16-net-zero-pulling-plug-americas-electrical-life-support.html"
          "http://company.com/author/newseditors"
          "http://company.com/2024-02-01-states-likely-to-suffer-power-grid-collapse.html"
          "http://company.com/author/oliviacook"
          "http://company.com/2024-01-26-utility-bills-record-highs-heating-home-luxury.html"
          "http://company.com/2024-01-24-solar-storm-kill-population-year-quaid-warns.html"
          "http://company.com/author/isabelle"
          "http://company.com/2024-01-23-sixt-drops-tesla-evs-poor-resale-value.html"
          "http://company.com/2024-01-18-texas-electric-grid-collapse-intense-winter-weather.html"
          "http://company.com/2024-01-12-appeals-court-cancels-bidens-capricious-energy-regulations.html"
          "http://company.com/author/bellecarter"
          "http://company.com/2024-01-03-scotlands-wind-turbines-using-generators-fossil-fuels.html"
          "http://company.com/author/zoeysky"
          "http://company.com/2023-12-22-rolls-royce-mini-nuclear-power-plants-ukraine.html"
          "http://company.com/author/richardbrown"
          "http://company.com/2023-12-19-new-study-charging-electric-vehicle-more-expensive.html"
          "http://company.com/author/lauraharris"
          "http://company.com/2023-12-13-chinas-4th-generation-nuclear-plant-fully-operational.html"
          "http://company.com/2023-12-11-germany-greens-deforestation-enchanted-forest-wind-turbines.html"
          "http://company.com/2023-12-06-green-energy-shift-disrupts-power-grid-reliability.html"
          "http://company.com/2023-12-04-plug-power-shares-plummet-premarket-trading-ny.html"
          "http://company.com/2023-11-30-ford-resumes-michigan-ev-plant-reduces-capacity.html"
          "http://company.com/2023-11-27-things-you-need-to-prepare-blackouts-emergencies.html"
          "http://company.com/2023-11-21-me-war-becomes-obsolete-once-lenr-allowed.html"
          "http://company.com/all-posts"]
         (take 30 (#'sut/find-all-links "http://company.com/" sample2)))))


(take 3 (#'sut/find-all-links "http://company.com/news" sample3))

(deftest poor-man-rss-html-test
  (testing "urls are prefixed by blog"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] sample1)]
      (is (= [{:link
               "http://company.com/blog/company-selected-as-a-venture-atlanta-2023-presenting-company/",
               :title
               "company.com Selected as a Venture Dallas 2023 Presenting Company - 14 Sep 2023 - company.com",
               :description nil,
               :published-date #inst "2023-09-14T00:00:00.000-00:00"}
              {:link
               "http://company.com/blog/betting-the-company-on-clojurescript/",
               :title
               "Betting the Company on ClojureScript - 20 Feb 2019 - Anthony Maley",
               :description "Betting the Company on ClojureScript",
               :published-date #inst "2019-02-20T00:00:00.000-00:00"}
              {:link
               "http://company.com/blog/company-io-recognized-as-one-of-atlantas-fastest-growing-companies-at-the-2022-atlanta-pacesetter-awards/",
               :title
               "company.com recognized as one of Atlanta's fastest-growing companies at the 2022 Atlanta Pacesetter Awards - 29 Apr 2022 - company.com",
               :description nil,
               :published-date #inst "2022-04-29T00:00:00.000-00:00"}]
             (take 3 (sut/poor-man-rss-html "http://company.com/blog"))))))

  (testing "handle url without '/'"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] sample2)]
      (is (= [{:link
               "http://company.com/2024-01-18-evs-struggle-with-depleted-batteries-freezing-temperature.html",
               :title nil,
               :description nil,
               :published-date nil}
              {:link
               "http://company.com/2023-12-19-new-study-charging-electric-vehicle-more-expensive.html",
               :title
               "New study: Charging an EV is equivalent to filling up a traditional car with gasoline worth $17.33 per gallon",
               :description nil,
               :published-date #inst "2023-12-19T00:00:00.000-00:00"}
              {:link
               "http://company.com/2023-12-11-germany-greens-deforestation-enchanted-forest-wind-turbines.html",
               :title
               "TRAVESTY: Germany’s Greens commence deforestation of Enchanted Forest to make way for wind turbines",
               :description nil,
               :published-date #inst "2023-11-12T00:00:00.000-00:00"}
              {:link
               "http://company.com/2023-12-04-plug-power-shares-plummet-premarket-trading-ny.html",
               :title
               "Renewable energy company Plug Power’s shares PLUMMET by 30% in premarket trading in New York",
               :description nil,
               :published-date #inst "2023-04-12T00:00:00.000-00:00"}
              {:link "http://company.com/author/ethanh",
               :title "By Ethan Huff",
               :description nil,
               :published-date #inst "2023-11-20T00:00:00.000-00:00"}]
             (take 5 (sut/poor-man-rss-html "http://company.com/"))))))
  (testing "handle url without the suffix of the root-url being similar to the prefix of the link"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] sample3)]
      (is (= [{:link
               "http://company.com/news/effect-photonics-to-become-the-most-highly-vertically-integrated-independent-coherent-optical-module-vendor",
               :title
               "Mar 8, 2022 - Press-release - EFFECT Photonics to become the most highly vertically integrated, independent coherent optical module vendor - From this acquisition, EFFECT Photonics will now own the entire coherent technology stack of all optical functions, including a high-performance tunable laser, together with DSP and FEC. This will enable the company to deliver on its ambition to make high performance coherent communications solutions widely accessible and affordable. Furthermore, it will enable longer term economic and environmentally sustainable communications due to the ability to deliver high-end performance and reach within a small footprint and with lower power consumption. This opens the way to drive coherent technology into new places, revolutionizing the way the world interconnects.",
               :description
               "From this acquisition, EFFECT Photonics will now own the entire coherent technology stack of all optical functions, including a high-performance tunable laser, together with DSP and FEC. This will enable the company to deliver on its ambition to make high performance coherent communications solutions widely accessible and affordable. Furthermore, it will enable longer term economic and environmentally sustainable communications due to the ability to deliver high-end performance and reach within a small footprint and with lower power consumption. This opens the way to drive coherent technology into new places, revolutionizing the way the world interconnects.",
               :published-date nil}
              {:link
               "http://company.com/news/axelera-launches-seed-round",
               :title
               "Sep 15, 2021 - Press-release - Dutch AI semiconductor startup Axelera AI launches with $12 million seed round - Stealth company incubated by blockchain unicorn Bitfury Group and global nanoelectronics R &D center imec launches with $12 million seed round",
               :description
               "Stealth company incubated by blockchain unicorn Bitfury Group and global nanoelectronics R &D center imec launches with $12 million seed round",
               :published-date #inst "2021-09-15T00:00:00.000-00:00"}
              {:link
               "http://company.com/news/dutch-spatial-ald-company-sparknano-secures-further-funding-to-scale-up-spatial-atomic-layer-deposition-for-energy-applications",
               :title
               "Nov 3, 2022 - Press-release - Dutch Spatial-ALD company SparkNano secures further funding to scale-up Spatial Atomic Layer Deposition for energy applications - SparkNano B.V. announced today that it has closed a funding round of 5.5M EUR. The investment was led by Air Liquide Venture Capital (ALIAD, France) and was supported by Dutch investors Somerset Capital Partners and Invest-NL as well as the existing investors Innovation Industries, the Brabant Development Company (BOM) and TNO.",
               :description
               "SparkNano B.V. announced today that it has closed a funding round of 5.5M EUR. The investment was led by Air Liquide Venture Capital (ALIAD, France) and was supported by Dutch investors Somerset Capital Partners and Invest-NL as well as the existing investors Innovation Industries, the Brabant Development Company (BOM) and TNO.",
               :published-date nil}]
             (take 3 (sut/poor-man-rss-html "http://company.com/news")))))))




