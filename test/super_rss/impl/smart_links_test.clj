(ns super-rss.impl.smart-links-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [is deftest testing]]
            [super-rss.impl.smart-links :as sut]
            [super-rss.html :as rss.html]))

(defn load-html [file-path]
  (slurp (io/resource file-path)))

(defn html->content-tree [body url]
  (#'rss.html/body->content-tree body url))

(def sample1 (load-html "smart_links/sample1.html"))
(def sample2 (load-html "smart_links/sample2.html"))
(def sample3 (load-html "smart_links/sample3.html"))

(deftest find-all-links-test
  (is (= ["http://company.com/framework/"
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
         (#'sut/find-all-links "http://company.com/" (html->content-tree sample1 "http://company.com/")))
      "Links are found")
  (is (= ["http://company.com/blog/framework/"
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
         (#'sut/find-all-links "http://company.com/blog" (html->content-tree sample1 "http://company.com/blog")))
      "Root url is preserved in each relative URL")
  (is (= ["http://company.com/2024-03-15-america-running-out-Elec-big-tech-demand-power.html"
          "http://company.com/2024-03-01-biden-green-tax-credit-plan-gift-china.html"
          "http://company.com/2024-02-19-environmentally-unfriendly-ev-roads-strain-infrastructure.html"
          "http://company.com/2024-02-16-net-zero-pulling-plug-americas-electrical-life-support.html"
          "http://company.com/2024-02-01-states-likely-to-suffer-power-grid-collapse.html"
          "http://company.com/2024-01-26-utility-bills-record-highs-heating-home-luxury.html"
          "http://company.com/2024-01-24-solar-storm-kill-population-year-quaid-warns.html"
          "http://company.com/2024-01-23-sixt-drops-tesla-evs-poor-resale-value.html"
          "http://company.com/2024-01-18-texas-electric-grid-collapse-intense-winter-weather.html"
          "http://company.com/2024-01-12-appeals-court-cancels-bidens-capricious-energy-regulations.html"
          "http://company.com/2024-01-03-scotlands-wind-turbines-using-generators-fossil-fuels.html"
          "http://company.com/2023-12-22-rolls-royce-mini-nuclear-power-plants-ukraine.html"
          "http://company.com/2023-12-19-new-study-charging-electric-vehicle-more-expensive.html"
          "http://company.com/2023-12-13-chinas-4th-generation-nuclear-plant-fully-operational.html"
          "http://company.com/2023-12-11-germany-greens-deforestation-enchanted-forest-wind-turbines.html"
          "http://company.com/2023-12-06-green-energy-shift-disrupts-power-grid-reliability.html"
          "http://company.com/2023-12-04-plug-power-shares-plummet-premarket-trading-ny.html"
          "http://company.com/2023-11-30-ford-resumes-michigan-ev-plant-reduces-capacity.html"
          "http://company.com/2023-11-27-things-you-need-to-prepare-blackouts-emergencies.html"
          "http://company.com/2023-11-21-me-war-becomes-obsolete-once-lenr-allowed.html"
          "http://company.com/2024-03-10-hydrogen-fuel-cells-could-save-ev-industry.html"
          "http://company.com/2024-02-19-putin-is-the-new-climate-change.html"
          "http://company.com/2024-02-18-texas-Elec-grid-demand-winter-storm-uri.html"
          "http://company.com/2024-02-05-heartless-globalists-celebrate-demise-poor-people-collapse.html"
          "http://company.com/2024-01-30-jury-orders-pacificorp-pay-62m-damages-wildfires.html"
          "http://company.com/2024-01-25-nyc-drivers-long-waiting-times-ev-chargers.html"
          "http://company.com/2024-01-23-germanys-Elec-price-doubles-green-nightmare-kicks-in.html"
          "http://company.com/2024-01-18-evs-struggle-with-depleted-batteries-freezing-temperature.html"
          "http://company.com/2024-01-18-623-million-grants-ev-charging-network-construction.html"
          "http://company.com/2024-01-05-uk-homes-left-without-power-storm-henk.html"
          "http://company.com/2024-01-01-just-stop-oil-climate-protesters-billions-die.html"
          "http://company.com/2023-12-20-rosemount-minnesota-15-minute-city-zuckerberg-meta.html"
          "http://company.com/2023-12-15-uae-bill-gates-build-advanced-nuclear-reactors.html"
          "http://company.com/2023-12-11-ev-charging-power-usage-exceeds-280-homes.html"
          "http://company.com/2023-12-10-no-ev-chargers-completed-under-nevi-plan.html"
          "http://company.com/2023-12-06-peter-koenig-west-energy-suicide-globalist-agenda.html"
          "http://company.com/2023-12-04-countries-investing-nuclear-energy-america-languishing-renewables.html"
          "http://company.com/2023-11-29-insiders-warn-dangers-of-going-green-catastrophic.html"
          "http://company.com/2023-11-22-greatest-threats-preppers-in-us-are-facing.html"
          "http://company.com/2023-11-20-charging-stations-use-energy-280-homes-hourly.html"]
         (#'sut/find-all-links "http://company.com/" (html->content-tree sample2 "http://company.com/")))))

(deftest poor-man-rss-html-test
  (testing "urls are prefixed by blog"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] (html->content-tree sample1 "http://company.com/blog"))]
      (is (= (->> [{:link
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
                    :published-date #inst "2022-04-29T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/a-simple-tool-for-load-testing-stateful-systems-using-clojure/",
                    :title
                    "A simple tool for load testing stateful systems using Clojure - 13 Jan 2022 - Bernard Labno",
                    :description
                    "A simple tool for load testing stateful systems using Clojure",
                    :published-date #inst "2022-01-13T00:00:00.000-00:00"}
                   {:link "http://company.com/blog/reversing-sherlock/",
                    :title
                    "Reversing Sherlock - 24 Jun 2020 - Anthony Maley",
                    :description "Reversing Sherlock",
                    :published-date #inst "2020-06-24T00:00:00.000-00:00"}
                   {:link "http://company.com/blog/return-of-the-fob/",
                    :title "Return of the fob - 25 Mar 2021 - Anthony Maley",
                    :description "Return of the fob",
                    :published-date #inst "2021-03-25T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/develop-a-digital-key-app-using-company-sdk/",
                    :title
                    "Develop a Digital Key app using company.com - 23 Nov 2021 - Michelle Lim",
                    :description nil,
                    :published-date #inst "2021-11-23T00:00:00.000-00:00"}
                   {:link "http://company.com/blog/the-value-of-principles/",
                    :title
                    "The value of principles - 16 Dec 2021 - João Paulo Soares",
                    :description "The value of principles",
                    :published-date #inst "2021-12-16T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/how-we-use-react-context-to-customize-our-apps-for-our-clients/",
                    :title
                    "How we use React Context to customize our apps for our clients - 07 Dec 2021 - Heather Haylett",
                    :description
                    "How we use React Context to customize our apps for our clients",
                    :published-date #inst "2021-12-07T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/finding-our-rhythm-a-creative-approach-to-documentation-woes/",
                    :title
                    "Finding Our Rhythm - A Creative Approach to Documentation Woes - 24 Apr 2023 - Jordan Miller with Heather Haylett",
                    :description
                    "Finding Our Rhythm - A Creative Approach to Documentation Woes",
                    :published-date #inst "2023-04-24T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/developing-mobile-digital-key-applications-with-clojurescript/",
                    :title
                    "Developing mobile digital key applications with ClojureScript - 20 May 2021 - David Nolen",
                    :description nil,
                    :published-date #inst "2021-05-20T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/is-1-password-still-1-password-too-many/",
                    :title
                    "Is \"1\" password still \"1\" password too many? - 18 Feb 2019 - Anthony Maley",
                    :description
                    "Is \"1\" password still \"1\" password too many?",
                    :published-date #inst "2019-02-18T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/writing-a-simple-mcumgr-client-with-rust/",
                    :title
                    "Writing a simple mcumgr client with Rust - 03 May 2023 - Frank Buss",
                    :description nil,
                    :published-date #inst "2023-05-03T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/blog/company-expands-into-tiruchirappalli-india/",
                    :title
                    "Expanding Horizons and embracing opportunities in Tiruchirappalli, India - 22 Jul 2023 - Anthony Maley",
                    :description
                    "Expanding Horizons and embracing opportunities in Tiruchirappalli, India",
                    :published-date #inst "2023-07-22T00:00:00.000-00:00"}
                   ;; TODO should not be found
                   {:link "http://company.com/blog/framework/",
                    :title "Framework",
                    :description nil,
                    :published-date nil}]
                  (sort-by :link)
                  #_(take 15))
             (->> (sut/poor-man-rss-html "http://company.com/blog")
                  (sort-by :link))))))

  (testing "handle url without '/'"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] (html->content-tree sample2 "http://company.com/"))]
      (is (= (->> [{:link
                    "http://company.com/2023-11-21-me-war-becomes-obsolete-once-lenr-allowed.html",
                    :title
                    "War in the Middle East will become OBSOLETE once Low Energy Nuclear Reactions are allowed to flourish, providing low-cost energy to the world",
                    :description nil,
                    :published-date #inst "2023-06-12T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/2023-11-27-things-you-need-to-prepare-blackouts-emergencies.html",
                    :title
                    "Things you need to prepare in order to deal with blackouts and emergencies",
                    :description nil,
                    :published-date #inst "2023-11-27T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/2023-11-30-ford-resumes-michigan-ev-plant-reduces-capacity.html",
                    :title
                    "Ford resumes Michigan EV battery plant but reduces production capacity by 40%, drops about 800 jobs",
                    :description nil,
                    :published-date #inst "2023-11-30T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/2023-12-04-plug-power-shares-plummet-premarket-trading-ny.html",
                    :title
                    "Renewable energy company Plug Power’s shares PLUMMET by 30% in premarket trading in New York",
                    :description nil,
                    :published-date #inst "2023-04-12T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/2023-12-06-green-energy-shift-disrupts-power-grid-reliability.html",
                    :title
                    "Insiders warn shifting to green energy could disrupt power grid reliability",
                    :description nil,
                    :published-date #inst "2023-06-12T00:00:00.000-00:00"}
                   {:link
                    "http://company.com/2023-12-06-peter-koenig-west-energy-suicide-globalist-agenda.html",
                    :title
                    "Peter Koenig: West’s ENERGY SUICIDE is part of globalists’ agenda",
                    :description nil,
                    :published-date #inst "2023-10-12T00:00:00.000-00:00"}]
              (sort-by :link))
             (->> (sut/poor-man-rss-html "http://company.com/")
                  (sort-by :link)
                  (take 6))))))
  (testing "handle url without the suffix of the root-url being similar to the prefix of the link"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] (html->content-tree sample3 "http://company.com/news"))]
      (let [results (sort-by :link (sut/poor-man-rss-html "http://company.com/news"))]
        (is (= [{:link "http://company.com/news/autofill-technologies-pre-series-a-funding",
                 :title
                 "Oct 28, 2021 - Press-release - AutoFill Technologies pre-Series A funding - AutoFill, a Dutch deep tech company specialising in inspection workflow processes for the automotive and rail industries, has secured €2.6 million in pre-series A funding.",
                 :description
                 "AutoFill, a Dutch deep tech company specialising in inspection workflow processes for the automotive and rail industries, has secured €2.6 million in pre-series A funding.",
                 :published-date #inst "2021-10-28T00:00:00.000-00:00"}
                {:link "http://company.com/news/axelera-launches-seed-round",
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
               (take 3 results)))
        ;; TODO this 3 should not be found
        ;; A way to adress this is to say that if some contents are found with description or published-date, then the items without can be removed
        (is (= [{:link "http://company.com/news/responsible-investment-strategy",
                  :title "Investment Strategy",
                  :description nil,
                  :published-date nil}
                 {:link "http://company.com/news/sustainability-related-disclosures",
                  :title "Sustainability Related Disclosures",
                  :description nil,
                  :published-date nil}
                 {:link "http://company.com/news/team", :title "Team - VI", :description nil, :published-date nil}]
               (take-last 3 results)))))))
