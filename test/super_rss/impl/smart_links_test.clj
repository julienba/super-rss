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
          "http://company.com/all-posts"
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
         (#'sut/find-all-links "http://company.com/" sample2))))

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
               :published-date #inst "2023-07-22T00:00:00.000-00:00"}]
             (sut/poor-man-rss-html "http://company.com/blog")))))

  (testing "handle url without '/'"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] sample2)]
      (is (= [{:link "http://company.com/2023-12-04-countries-investing-nuclear-energy-america-languishing-renewables.html",
               :title
               "Many countries investing in nuclear energy generation while America LANGUISHES in pursuit of less capable renewables",
               :description nil,
               :published-date #inst "2023-04-12T00:00:00.000-00:00"}
              {:link "http://company.com/2024-01-18-evs-struggle-with-depleted-batteries-freezing-temperature.html",
               :title
               "GREEN FAIL: EV owners struggle with depleted batteries and long charging station lines amid freezing temperature in Chicago",
               :description nil,
               :published-date #inst "2024-01-23T00:00:00.000-00:00"}
              {:link "http://company.com/2023-12-19-new-study-charging-electric-vehicle-more-expensive.html",
               :title
               "New study: Charging an EV is equivalent to filling up a traditional car with gasoline worth $17.33 per gallon",
               :description nil,
               :published-date #inst "2023-12-19T00:00:00.000-00:00"}
              {:link "http://company.com/2023-12-11-germany-greens-deforestation-enchanted-forest-wind-turbines.html",
               :title "TRAVESTY: Germany’s Greens commence deforestation of Enchanted Forest to make way for wind turbines",
               :description nil,
               :published-date #inst "2023-11-12T00:00:00.000-00:00"}
              {:link "http://company.com/2023-12-04-plug-power-shares-plummet-premarket-trading-ny.html",
               :title "Renewable energy company Plug Power’s shares PLUMMET by 30% in premarket trading in New York",
               :description nil,
               :published-date #inst "2023-04-12T00:00:00.000-00:00"}
              {:link "http://company.com/2023-11-27-things-you-need-to-prepare-blackouts-emergencies.html",
               :title "Things you need to prepare in order to deal with blackouts and emergencies",
               :description nil,
               :published-date #inst "2023-11-27T00:00:00.000-00:00"}
              {:link "http://company.com/2024-03-01-biden-green-tax-credit-plan-gift-china.html",
               :title
               "Experts warn that Joe Biden’s green tax credit plan is a GIFT TO CHINA – at the expense of U.S. manufacturing",
               :description nil,
               :published-date #inst "2024-01-03T00:00:00.000-00:00"}
              {:link "http://company.com/2024-01-12-appeals-court-cancels-bidens-capricious-energy-regulations.html",
               :title
               "Appeals court cancels Biden’s CAPRICIOUS energy regulatory actions targeting dishwashers and washing machines",
               :description nil,
               :published-date #inst "2024-12-01T00:00:00.000-00:00"}
              {:link "http://company.com/2024-01-01-just-stop-oil-climate-protesters-billions-die.html",
               :title "If we “just stop oil” like climate protesters want, six BILLION people could die",
               :description nil,
               :published-date #inst "2024-01-01T00:00:00.000-00:00"}
              {:link "http://company.com/2023-12-06-peter-koenig-west-energy-suicide-globalist-agenda.html",
               :title "Peter Koenig: West’s ENERGY SUICIDE is part of globalists’ agenda",
               :description nil,
               :published-date #inst "2023-10-12T00:00:00.000-00:00"}]
             (take 10 (sut/poor-man-rss-html "http://company.com/"))))))
  (testing "handle url without the suffix of the root-url being similar to the prefix of the link"
    (with-redefs [rss.html/get-hickory-web-page (fn [_] sample3)]
      (is (= [{:link
               "http://company.com/news/effect-photonics-to-become-the-most-highly-vertically-integrated-independent-coherent-optical-module-vendor",
               :title
               "Mar 8, 2022 - Press-release - EFFECT Photonics to become the most highly vertically integrated, independent coherent optical module vendor - From this acquisition, EFFECT Photonics will now own the entire coherent technology stack of all optical functions, including a high-performance tunable laser, together with DSP and FEC. This will enable the company to deliver on its ambition to make high performance coherent communications solutions widely accessible and affordable. Furthermore, it will enable longer term economic and environmentally sustainable communications due to the ability to deliver high-end performance and reach within a small footprint and with lower power consumption. This opens the way to drive coherent technology into new places, revolutionizing the way the world interconnects.",
               :description
               "From this acquisition, EFFECT Photonics will now own the entire coherent technology stack of all optical functions, including a high-performance tunable laser, together with DSP and FEC. This will enable the company to deliver on its ambition to make high performance coherent communications solutions widely accessible and affordable. Furthermore, it will enable longer term economic and environmentally sustainable communications due to the ability to deliver high-end performance and reach within a small footprint and with lower power consumption. This opens the way to drive coherent technology into new places, revolutionizing the way the world interconnects.",
               :published-date nil}
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
               :published-date nil}
              {:link "http://company.com/news/lionvolt-closes-eu4m-seed-round",
               :title
               "Nov 18, 2021 - Press-release - LionVolt Closes €4M Seed Round - European battery start-up LionVolt announced today it has successfully closed a seed round of €4 million, bringing its total funding this year to more than €5 million. ",
               :description
               "European battery start-up LionVolt announced today it has successfully closed a seed round of €4 million, bringing its total funding this year to more than €5 million. ",
               :published-date #inst "2021-11-18T00:00:00.000-00:00"}
              {:link "http://company.com/news/lionvolt-3d-technology",
               :title
               "Jul 22, 2021 - Press-release - LionVolt to enter the battery industry with revolutionary 3D technology - LionVolt raises 1.1 million euros in funding from Innovation Industries, Goeie Grutten, Brabant Startup Fund (BSF) and the Brabant Development Company (BOM) to accelerate the development of its 3D battery technology.",
               :description
               "LionVolt raises 1.1 million euros in funding from Innovation Industries, Goeie Grutten, Brabant Startup Fund (BSF) and the Brabant Development Company (BOM) to accelerate the development of its 3D battery technology.",
               :published-date #inst "2021-07-22T00:00:00.000-00:00"}
              {:link "http://company.com/news/onera-raises-series-b",
               :title
               "Jul 29, 2021 - Press-release - Onera raises €10.5M in Series B funding - Onera Health, a leader in sleep diagnostic and monitoring solutions, closed €10.5M in Series B funding. The round was led by Innovation Industries in close collaboration with Invest-NL and with existing investors imec.xpand, Jazz Pharmaceuticals, BOM and 15th Rock.",
               :description
               "Onera Health, a leader in sleep diagnostic and monitoring solutions, closed €10.5M in Series B funding. The round was led by Innovation Industries in close collaboration with Invest-NL and with existing investors imec.xpand, Jazz Pharmaceuticals, BOM and 15th Rock.",
               :published-date #inst "2021-07-29T00:00:00.000-00:00"}
              {:link "http://company.com/news/portfolio-company-phix-enters-next-growth-phase",
               :title
               "Jul 27, 2022 - News - Portfolio company PhiX enters next growth phase - To continue to support their customers, PHIX secured €3 million in new capital for the short term and €20 million for the long term from the National Growth Fund. In addition, PHIX will occupy a new building to support further industrialization and scaling of photonic chip packaging in large volumes.",
               :description nil,
               :published-date #inst "2022-07-27T00:00:00.000-00:00"}
              {:link "http://company.com/news/protealis-extended-a-round",
               :title
               "Sep 2, 2021 - Press-release - Sustainable protein seed start-up Protealis doubles capital upon closing its extended A-round - The Ghent-based company Protealis, a spin-off from VIB and ILVO, was incorporated earlier this year backed by 6 Mio EUR VC funds. Now, Protealis raised an additional 5.7 Mio EUR in its extended A-round.",
               :description
               "The Ghent-based company Protealis, a spin-off from VIB and ILVO, was incorporated earlier this year backed by 6 Mio EUR VC funds. Now, Protealis raised an additional 5.7 Mio EUR in its extended A-round.",
               :published-date nil}
              {:link
               "http://company.com/news/enocean-secures-a-significant-investment-from-innovation-industries-to-drive-sustainable-iot-innovation",
               :title
               "Sep 26, 2023 - Press-release - EnOcean secures a significant investment from Innovation Industries to drive sustainable IoT innovation - EnOcean announces a substantial investment from Innovation Industries, a leading deep tech VC fund. This strategic partnership signifies a strong alignment of strategic goals between the two entities and provides EnOcean with the necessary capital to support its continued growth.",
               :description
               "EnOcean announces a substantial investment from Innovation Industries, a leading deep tech VC fund. This strategic partnership signifies a strong alignment of strategic goals between the two entities and provides EnOcean with the necessary capital to support its continued growth.",
               :published-date #inst "2023-09-26T00:00:00.000-00:00"}
              {:link "http://company.com/news/innovation-industries-co-leads-series-a-round-of-beeoled",
               :title
               "Aug 23, 2023 - News - Innovation Industries co-leads Series A round of beeOLED - beeOLED raises EUR 13.3m in Series A funding from world-class deep tech investors to solve the last big challenge of the OLED industry. The German deep tech startup will use the investment to further develop its innovative, high-efficiency, deep-blue emitter technology towards go-to-market-readiness. ",
               :description nil,
               :published-date #inst "2023-08-23T00:00:00.000-00:00"}]
             (take 10 (sut/poor-man-rss-html "http://company.com/news")))))))
