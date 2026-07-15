(ns benchmarking.combined-benchmarking
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.tools.build.api :as build]
   [nextjournal.clerk :as clerk]
   [nextjournal.clerk-slideshow :as slideshow]
   [fastmath.core :as m]
   ;; [witan.send.benchmarking.regional-neighbours :as rn]
   [witan.send.benchmarking.statistical-neighbours :as sn]
   [tablecloth.api :as tc]
   [tech.v3.dataset.reductions :as dsr]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.s251.alleducation-la-regional-national :as s251]
   [witan.send.benchmarking.sen2-2025.newplans :as newplans]
   [witan.send.benchmarking.population-2025.population :as pop2025]
   [witan.send.population.england :as pop])
  (:import
   (java.time LocalDateTime)
   (java.time.format DateTimeFormatter)))

(def la-name "South Gloucestershire")
(def statistical-neighbours (sn/neighbours la-name))
(def statistical-neighbours-pred (sn/neighbours-name-pred la-name))
(def la-and-stat-neighbours-pred (conj statistical-neighbours-pred la-name))

(def out-dir "doc/")

(defn output-ns [ns]
  (let [ns-str (str ns)
        pathified-namepace (str/replace ns-str #"\.|-" {"." "/" "-" "_"})
        in-path (str "notebooks/" pathified-namepace ".clj")
        out-path (str out-dir
                      (-> la-name
                          (str/replace #"\.|-| |,"
                                       {"." "/"
                                        "-" "_"
                                        "," "_"
                                        " " "_"})
                          (str
                           "-financial-benchmarking"
                           "--"
                           (.format (LocalDateTime/now) DateTimeFormatter/ISO_DATE)
                           ".html")))
        index-out (str out-dir "index.html")]
    (clerk/build! {:paths    [in-path]
                   :ssr true
                   :bundle   true
                   :out-path out-dir})
    [(.renameTo (io/file index-out) (io/file out-path)) index-out out-path]))

^::clerk/no-cache
(def build-string
  (format "Built from commit %s on %s"
          (build/git-process {:git-args "rev-parse --short HEAD"})
          (DateTimeFormatter/.format DateTimeFormatter/ISO_LOCAL_DATE (LocalDateTime/now))))

(comment

  (do
    (clerk/clear-cache!)
    (when-let [path (output-ns *ns*)]
      (clerk/show! *file*)
      path))

;;; Output NS

  )

(clerk/add-viewers! [slideshow/viewer])

(def mc-logo-url "https://mastodonc.com/assets/images/logo_mastodonc.png")

(defn mc-logo []
  (clerk/html
   {::clerk/width :full}
   [:div.fixed.bottom-0.right-12 [:img {:src mc-logo-url}]]))

(defn watermark []
  (clerk/html
   {::clerk/width :full}
   [:div.fixed.bottom-0.left-12 [:p.font-sans.italic la-name]]))

(
;;; Charting Helpers
 )

(defn neighbour-comparison-boxplot
  [{:keys [neighbour-data la-name title y-field y-title x-field x-title max-y series-name]}]
  (let [la-plans (-> neighbour-data
                     (tc/select-rows #(#{la-name} (series-name %))))
        box-data
        (transduce
         identity
         (fn
           ([] {})
           ([acc]
            (into []
                  (map (fn [[k v]]
                         {:x k
                          :y (:y v)
                          :text (:text v)
                          :name k
                          :marker {:color "orange"}
                          :boxpoints "all"
                          :pointpos -1.8
                          :jitter 0.3
                          :type "box"}))
                  acc))
           ([acc x]
            (-> acc
                (update-in [(x-field x) :y] conj (y-field x))
                (update-in [(x-field x) :text] conj (series-name x)))))
         (-> neighbour-data
             (tc/drop-rows #(#{la-name} (series-name %)))
             (tc/rows :as-maps)))]
    {:data (into
            ;; The order of the x points here determine the order of the x-axis
            [{:x (into [] (la-plans x-field))
              :y (into [] (la-plans y-field))
              :text (into [] (la-plans series-name))
              :name la-name
              :marker {:color "blue" :size 14 :symbol "star-diamond"}
              :mode "markers"
              :type "scatter"}]
            box-data)
     :layout {:title {:text title}
              :scattermode "group"
              :scattergap 0.7
              :xaxis {:dtick 1 :title x-title}
              :yaxis {:rangemode "tozero" :title y-title :range (when max-y [0 max-y])}
              :height 600
              :width 1400
              :showlegend false}
     :config {:displayModeBar false
              :displayLogo false}}))

(defn phase-from-integer-age [age]
  (cond
    (#{0 1 2 3} age) :early_years_establishments
    (#{4 5 6 7 8 9 10} age) :primary_schools
    (#{11 12 13 14 15 16} age) :secondary_schools
    (#{17 18 19 20 21 22 23 24} age) :post_16))

(

 ;; DECISION: We're going to use 0-25 population as our denominator for
 ;; benchmarking comparison of SEND Spending

 ;; So, the 2024/25 Financial year covers April, May, June, July,
 ;; August, September, October, November, December in 2024, which is 9
 ;; months and January, February, March in 2025 which is 3 months, so
 ;; the population for the 2024/25 Financial year is:
 ;;
 ;; (+ (* (/ 9 12) pop-2024) (* (/ 3 12) pop-2025))

 )

(defn year-to-financial-year [year]
  (format "FY %d/%d" year (-> year (rem 100) inc)))

(def send-age-pop-by-la-calendar-year
  (pop2025/table
   :la-name-fn la-and-stat-neighbours-pred
   :pipeline-fn
   #(-> %
        pop2025/pop-by-age-per-financial-year
        (tc/order-by [:geo-name :financial-year]))))

(def send-age-pop-by-la-per-financial-year
  (pop2025/table
   :la-name-fn la-and-stat-neighbours-pred
   :pipeline-fn
   #(-> %
        pop2025/pop-by-age-per-financial-year
        (pop2025/pop-total-by-year :financial-year :financial-year-pop)
        (tc/order-by [:geo-name :financial-year]))))

(def total-pop-by-phase-by-la-calendar-year
  (pop2025/table
   :la-name-fn la-and-stat-neighbours-pred
   :pipeline-fn
   #(-> %
        pop2025/pop-by-age-per-financial-year
        pop2025/pop-by-phase-per-financial-year
        (as-> $
            (dsr/group-by-column-agg
             [:geo-code :geo-name :financial-year :phase]
             {:total-financial-year-pop (dsr/sum :financial-year-pop)}
             $))
        (tc/order-by [:geo-name :financial-year :phase])))
;;; NEXT
  #_(-> (pop/->dataset)
        (tc/map-columns :phase [:age] phase-from-integer-age)
        (tc/rename-columns {:ctyua23cd :geo-code
                            :ctyua23nm :geo-name})
        (as-> $
            (dsr/group-by-column-agg
             [:geo-code :geo-name :year :calendar-year :phase]
             {:total-pop (dsr/sum :population)}
             $))))

(defn total-pop-by-phase-by-la-financial-year [population-by-cy]
  (-> (tc/inner-join
       (-> population-by-cy
           (tc/map-columns :y1-pop [:total-pop] #(* (/ 9 12) %))
           (tc/map-columns :financial-year [:calendar-year]
                           year-to-financial-year))
       (-> population-by-cy
           (tc/map-columns :y2-pop [:total-pop] #(* (/ 3 12) %))
           (tc/map-columns :financial-year [:calendar-year]
                           #(-> % dec year-to-financial-year)))
       [:geo-code :financial-year :phase])
      (tc/select-columns [:geo-code :geo-name
                          :phase
                          #_:calendar-year #_:right.calendar-year
                          :financial-year #_:right.financial-year
                          #_:total-pop #_:right.total-pop
                          :y1-pop :y2-pop])
      (tc/map-columns :financial-year-pop [:y1-pop :y2-pop] #(int (+ %1 %2)))))

(def send-age-pop-by-phase-by-la-per-financial-year
  (-> total-pop-by-phase-by-la-calendar-year
      total-pop-by-phase-by-la-financial-year
      #_(tc/select-rows #(la-and-stat-neighbours-pred (:geo-name %)))))

(
;;; ## Calculation Helper
 )

(defn calculate [& {:keys [numerator-ds
                           denominator-ds
                           join-keys
                           input-fields
                           value-fn
                           output-field
                           finalise-f]
                    :or {value-fn #(m/approx (dfn// %1 %2))}}]
  (-> numerator-ds
      (tc/inner-join denominator-ds join-keys)
      (tc/map-columns output-field input-fields value-fn)
      (cond-> 
          finalise-f (finalise-f))))

^{::clerk/visibility {:code :hide :result :hide}}
(defn financial-dimensions-f [category-of-expenditure-f setting-f]
  (fn [ds]
    (-> ds
        (tc/select-rows (fn [r] (category-of-expenditure-f (:category_of_expenditure r))))
        (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
        (s251/tidy-table)
        (tc/map-columns :time_period [:time_period] s251/format-financial-year)
        (tc/select-rows (fn [r] (setting-f (:setting r))))
        (as-> $
            (dsr/group-by-column-agg
             [:time_period :geo-code :geo-name]
             {:amount (dsr/sum :amount)}
             $)))))

^{::clerk/visibility {:code :hide :result :hide}}
(def default-financial-calc-map
  {:denominator-ds
   send-age-pop-by-la-per-financial-year
   :join-keys
   {:left [:time_period :geo-code]
    :right [:financial-year :geo-code]}
   :input-fields [:amount :financial-year-pop]
   :output-field :net-expenditure-per-send-age-cyp
   :finalise-f (fn [ds]
                 (-> ds
                     (tc/drop-columns #":right.*")
                     (tc/drop-columns [:financial-year :time_identifier :geographic_level])
                     (tc/order-by [:geo-code :time_period])))})


(
;;; Deck
 )
{::clerk/visibility {:result :show}}

(clerk/row
 {::clerk/width :full}
 (clerk/html
;;; Title Page
  {::clerk/width :full}
  [:div.max-w-screen-2xl.font-sans
   [:h1.text-6xl.font-extrabold.mb-12
    (format "SEN2 and S251 benchmarking results for %s" la-name)]
   [:p.text-4xl.font-bold.italic "Presented by Mastodon C"]
   [:p build-string]
   [:p.text-3xl.mt-12 "Use ⬅️➡️ keys to navigate and ESC to see an overview."]]))

(mc-logo)

;; ---
;;; ## Statistical Nearest Neighbours
(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> statistical-neighbours
      (tc/select-columns [:sn :sn_name :sn_prox])
      (tc/rename-columns {:sn_name "Neighbour Name"
                          :sn "Neighbour Rank"
                          :sn_prox "Statistical Proximity"}))))

(watermark)
(mc-logo)

;; ---
;;; # High Needs Amount per CYP of SEND Age (0-25)

(watermark)
(mc-logo)

;; ---
;;; ## Total Place Funding for Special Schools and AP/PRUs
;;
;; Source: Section 251 (2024/2025), Line 1.0.2,
;; sen_and_special_schools and pupil_referral_units_and_alt_provision
;; columns.


^{::clerk/visibility {:code :hide :result :hide}}
(def total-funding-for-schools-and-ap-prus
  (calculate
   (assoc default-financial-calc-map
          :numerator-ds 
          (s251/table
           :pipeline-fn
           (financial-dimensions-f
            #{"1.0.2 High needs place funding within Individual Schools Budget"}
            #{:sen_and_special_schools :pupil_referral_units_and_alt_provision})))))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data total-funding-for-schools-and-ap-prus
    :title "Total Place Funding for Special Schools and AP/PRUs per SEND age CYP"
    :la-name la-name
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Net Expenditure per SEND age CYP (£s)"}))
 (clerk/col 
  (clerk/md (format "#### Amount for %s" la-name))
  (clerk/table
   (-> total-funding-for-schools-and-ap-prus
       (tc/select-rows (fn [r] (= (:geo-name r) la-name)))
       (tc/select-columns [:time_period :amount])
       (tc/map-columns :amount [:amount] #(format "£%,12.0f" %))
       (tc/rename-columns {:time_period "Time Period" :amount "Amount"})))))

(watermark)
(mc-logo)

;; ---
;;; ## Top up funding (maintained schools, academies, free schools and colleges)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def top-up-funding-maintained-schools
  (calculate
   (assoc default-financial-calc-map
          :numerator-ds 
          (s251/table
           :pipeline-fn
           (financial-dimensions-f
            (some-fn
             (fn [s] (re-find #"^1.2.1 " s))
             (fn [s] (re-find #"^1.2.2 " s))
             (fn [s] (re-find #"^1.2.4 " s))
             (fn [s] (re-find #"^1.2.11 " s)))
            #{:gross_expenditure})))))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data top-up-funding-maintained-schools
    :la-name la-name
    :title "Top up funding (maintained schools, academies, free schools and colleges) per SEND age CYP"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"}))
 (clerk/col 
  (clerk/md (format "#### Amount for %s" la-name))
  (clerk/table
   (-> top-up-funding-maintained-schools
       (tc/select-rows (fn [r] (= (:geo-name r) la-name)))
       (tc/select-columns [:time_period :amount])
       (tc/map-columns :amount [:amount] #(format "£%,12.0f" %))
       (tc/rename-columns {:time_period "Time Period" :amount "Amount"})))))

(watermark)
(mc-logo)

;; ---
;;; ## Top up funding (non-maintained and independent schools and colleges)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def top-up-funding-non-maintained-and-independent-schools-and-colleges
  (calculate
   (assoc default-financial-calc-map
          :numerator-ds 
          (s251/table
           :pipeline-fn
           (financial-dimensions-f
            #(re-find #"1.2.3 " %)
            #{:gross_expenditure})))))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Top up funding (non-maintained and independent schools and colleges) per SEND age CYP"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"}))
 (clerk/col 
  (clerk/md (format "#### Amount for %s" la-name))
  (clerk/table
   (-> top-up-funding-maintained-schools
       (tc/select-rows (fn [r] (= (:geo-name r) la-name)))
       (tc/select-columns [:time_period :amount])
       (tc/map-columns :amount [:amount] #(format "£%,12.0f" %))
       (tc/rename-columns {:time_period "Time Period" :amount "Amount"})))))

(watermark)
(mc-logo)

;; ---
;;; ## SEN support and inclusion services
;;
;; Source: Section 251 (2024/2025), Lines 1.2.5, 1.2.8, and 1.2.9 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def sen-support-and-inclusion-services
  (calculate
   (assoc default-financial-calc-map
          :numerator-ds 
          (s251/table
           :pipeline-fn
           (financial-dimensions-f
            (some-fn
             (fn [s] (re-find #"^1.2.1 " s))
             (fn [s] (re-find #"^1.2.2 " s))
             (fn [s] (re-find #"^1.2.4 " s))
             (fn [s] (re-find #"^1.2.11 " s)))
            #{:gross_expenditure})))))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data sen-support-and-inclusion-services
    :la-name la-name
    :title "SEN support and inclusion services per SEND age CYP"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"}))
 (clerk/col 
  (clerk/md (format "#### Amount for %s" la-name))
  (clerk/table
   (-> sen-support-and-inclusion-services
       (tc/select-rows (fn [r] (= (:geo-name r) la-name)))
       (tc/select-columns [:time_period :amount])
       (tc/map-columns :amount [:amount] #(format "£%,12.0f" %))
       (tc/rename-columns {:time_period "Time Period" :amount "Amount"})))))

(watermark)
(mc-logo)

;; ---
;;; ## Alternative provision services
;;
;; Source: Section 251 (2024/2025), Lines 1.2.7 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def alternative-provision-services
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"^1.2.7 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:gross_expenditure}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data alternative-provision-services
    :la-name la-name
    :title "Alternative provision services"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Hospital education services
;;
;; Source: Section 251 (2024/2025), Lines 1.2.6 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def hospital-education-services
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"^1.2.6 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:gross_expenditure}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data hospital-education-services
    :la-name la-name
    :title "Hospital education services"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Therapies and other health related services
;;
;; Source: Section 251 (2024/2025), Lines 1.2.13 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def therapies-and-other-health-related-services
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"^1.2.13 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:gross_expenditure}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data therapies-and-other-health-related-services
    :la-name la-name
    :title "Therapies and other health related services"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; # High needs amount per pupil: place funding split by phase (for mainstream) and type of institution (for specialist provision)

;; ---
;;; ## Primary place funding per pupil
;;
;; Source: Section 251 (2024/2025), Line 1.0.2, primary_schools column

^{::clerk/visibility {:code :hide :result :hide}}
(def primary-place-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r] (#{"1.0.2 High needs place funding within Individual Schools Budget"}
                                       (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:primary_schools} (:setting r)))))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data primary-place-funding-per-pupil
    :la-name la-name
    :title "Primary place funding per pupil"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per Primary age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Secondary place funding per pupil
;;
;; Source: Section 251 (2024/2025), Line 1.0.2, secondary_schools column

^{::clerk/visibility {:code :hide :result :hide}}
(def secondary-place-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r] (#{"1.0.2 High needs place funding within Individual Schools Budget"}
                                       (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:secondary_schools} (:setting r)))))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data secondary-place-funding-per-pupil
    :la-name la-name
    :title "Secondary place funding per pupil"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per Secondary age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Special place funding per pupil
;;
;; Source: Section 251 (2024/2025), Line 1.0.2, sen_and_special_schools column

^{::clerk/visibility {:code :hide :result :hide}}
(def special-place-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r] (#{"1.0.2 High needs place funding within Individual Schools Budget"}
                                       (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:sen_and_special_schools} (:setting r)))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data special-place-funding-per-pupil
    :la-name la-name
    :title "Special place funding per pupil"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per SEND age (0-25) CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## PRU and AP place funding per pupil
;;
;; Source: Section 251 (2024/2025), Line 1.0.2, pupil_referral_units_and_alt_provision column

^{::clerk/visibility {:code :hide :result :hide}}
(def pru-and-ap-place-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r] (#{"1.0.2 High needs place funding within Individual Schools Budget"}
                                       (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:pupil_referral_units_and_alt_provision} (:setting r)))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data pru-and-ap-place-funding-per-pupil
    :la-name la-name
    :title "PRU and AP place funding per pupil"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per SEND age (0-25) CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; # High needs amount per pupil: top up funding (maintained schools, academies, free schools and colleges) split by phase (for mainstream) and type of institution (for specialist provision)


(watermark)
(mc-logo)

;; ---
;;; ## Early years top up funding per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11
^{::clerk/visibility {:code :hide :result :hide}}
(def early-years-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:early_years_establishments} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name :setting]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data early-years-top-up-funding-per-pupil
    :la-name la-name
    :title "Early years top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per Early Years age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Primary top up funding per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11

^{::clerk/visibility {:code :hide :result :hide}}
(def primary-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:primary_schools} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name :setting]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data primary-top-up-funding-per-pupil
    :la-name la-name
    :title "Primary top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per Primary age CYP (£s)"})))


(watermark)
(mc-logo)

;; ---
;;; ## Secondary top up funding per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11

^{::clerk/visibility {:code :hide :result :hide}}
(def secondary-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:secondary_schools} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name :setting]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data secondary-top-up-funding-per-pupil
    :la-name la-name
    :title "Secondary top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per Secondary age CYP (£s)"})))


(watermark)
(mc-logo)

;; ---
;;; ## Special top up funding per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11

^{::clerk/visibility {:code :hide :result :hide}}
(def special-schools-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:sen_and_special_schools} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data special-schools-top-up-funding-per-pupil
    :la-name la-name
    :title "Special top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))


(watermark)
(mc-logo)

;; ---
;;; ## Alternative provision top up funding per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11

^{::clerk/visibility {:code :hide :result :hide}}
(def alternative-provision-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:pupil_referral_units_and_alt_provision} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data alternative-provision-top-up-funding-per-pupil
    :la-name la-name
    :title "Alternative provision top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Post-school top up funding per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11 post_16 column


^{::clerk/visibility {:code :hide :result :hide}}
(def post-16-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:post_16} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name :setting]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data post-16-top-up-funding-per-pupil
    :la-name la-name
    :title "Post School top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Expenditure per Post School age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Top up funding income per pupil (maintained)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11 income column

^{::clerk/visibility {:code :hide :result :hide}}
(def income-top-up-funding-per-pupil
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (tc/select-rows (fn [r]
                                ((some-fn
                                  (fn [s] (re-find #"^1.2.1 " s))
                                  (fn [s] (re-find #"^1.2.2 " s))
                                  (fn [s] (re-find #"^1.2.4 " s))
                                  (fn [s] (re-find #"^1.2.11 " s)))
                                 (:category_of_expenditure r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r] (#{:income} (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data income-top-up-funding-per-pupil
    :la-name la-name
    :title "Income top up funding per pupil (maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-cyp
    :y-title "Gross Income per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; # High needs amount per pupil: top up funding (non-maintained schools and independent schools and colleges) split by phase (for mainstream) and type of institution (for specialist provision)

(watermark)
(mc-logo)

;; ---
;;; ## Early years top up funding per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def early-years-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:early_years_establishments}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name :setting]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data early-years-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Early years top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per Early Years age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Primary top up funding per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def primary-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:primary_schools}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name :setting]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data primary-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Primary top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per Primary age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Secondary top up funding per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def secondary-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:secondary_schools}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name :setting]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data secondary-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Secondary top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per Secondary age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Special top up funding per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def special-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:sen_and_special_schools}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data special-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Special top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Alternative provision top up funding per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def alternative-provision-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:pupil_referral_units_and_alt_provision}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data alternative-provision-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Alternative provision top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Post-school top up funding per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def post-16-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:post_16}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name :setting]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-phase-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code :setting]
        :right [:financial-year :geo-code :phase]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data post-16-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Post School top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per Post School age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; ## Top up funding income per pupil (non-maintained)
;;
;; Source: Section 251 (2024/2025), Line 1.2.3

^{::clerk/visibility {:code :hide :result :hide}}
(def income-top-up-funding-non-maintained-and-independent-schools-and-colleges
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]
                                    (re-find #"1.2.3 " (:category_of_expenditure r))))
                  (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                  (s251/tidy-table)
                  (tc/map-columns :time_period [:time_period] s251/format-financial-year)
                  (tc/select-rows (fn [r]
                                    (#{:income}
                                     (:setting r))))
                  (as-> $
                      (dsr/group-by-column-agg
                       [:time_period :geo-code :geo-name :setting]
                       {:amount (dsr/sum :amount)}
                       $))))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])
      (tc/order-by [:geo-code :time_period])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data income-top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Income top up funding per pupil (non-maintained)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Income per SEND age CYP (£s)"})))

(watermark)
(mc-logo)

;; ---
;;; # New EHC Plans

;; ---
;;; ## New EHC Plans Total

^{::clerk/visibility {:code :hide :result :hide}}
(def new-echps-total
  (-> (calculate
       :numerator-ds
       (let [setting :new_ehc_plans]
         (newplans/table
          :pipeline-fn
          (fn [ds]
            (-> ds
                (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                newplans/tidy-table
                (tc/select-rows (fn [r] (= "New EHC plans" (:breakdown r))))
                (tc/select-rows (fn [r] (= setting (:setting r))))))))
       :denominator-ds
       send-age-pop-by-la-calendar-year
       :join-keys
       {:left [:calendar-year :geo-code]
        :right [:year :geo-code]}
       :input-fields [:amount :total-pop]
       :output-field :new-ehcps-per-thousand
       :value-fn #(m/approx (dfn/* 1000 (dfn// %1 %2))))
      (tc/drop-columns #":right.*")
      (tc/order-by [:geo-code :calendar-year])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data new-echps-total
    :la-name la-name
    :title "New EHC Plans Total"
    :series-name :geo-name
    :x-field :calendar-year
    :x-title "Calendar Year (Jan-Dec)"
    :y-field :new-ehcps-per-thousand
    :y-title "New EHCPs per 1,000"}))
 (clerk/col
  (clerk/md "### Number of New EHCPs")
  (clerk/table
   (-> new-echps-total
       (tc/select-rows (fn [r] (= (:geo-name r) la-name)))
       (tc/select-columns [:calendar-year :amount])
       (tc/rename-columns {:calendar-year "Calendar Year (Jan-Dec)" :amount "New EHCPs"})))))

(watermark)
(mc-logo)

;; ---
;;; # New EHC Plans by Setting

;; ---
;;; # New EHCPs in Mainstream Settings

^{::clerk/visibility {:code :hide :result :hide}}
(def new-echps-mainstream-total
  (-> (calculate
       :numerator-ds
       (let [setting :mainstream_total]
         (newplans/table
          :pipeline-fn
          (fn [ds]
            (-> ds
                (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                newplans/tidy-table
                (tc/select-rows (fn [r] (= "New EHC plans" (:breakdown r))))
                (tc/select-rows (fn [r] (= setting (:setting r))))))))
       :denominator-ds
       send-age-pop-by-la-calendar-year
       :join-keys
       {:left [:calendar-year :geo-code]
        :right [:year :geo-code]}
       :input-fields [:amount :total-pop]
       :output-field :new-ehcps-per-thousand
       :value-fn #(m/approx (dfn/* 1000 (dfn// %1 %2))))
      (tc/drop-columns #":right.*")
      (tc/order-by [:geo-code :calendar-year])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data new-echps-mainstream-total
    :la-name la-name
    :title "New EHCPs in Mainstream Settings"
    :series-name :geo-name
    :x-field :calendar-year
    :x-title "Calendar Year (Jan-Dec)"
    :y-field :new-ehcps-per-thousand
    :y-title "New EHCPs per 1,000"})))

(watermark)
(mc-logo)

;; ---
;;; ## New EHCPs in Special Settings

^{::clerk/visibility {:code :hide :result :hide}}
(def new-echps-special-total
  (-> (calculate
       :numerator-ds
       (let [setting :special_total]
         (newplans/table
          :pipeline-fn
          (fn [ds]
            (-> ds
                (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                newplans/tidy-table
                (tc/select-rows (fn [r] (= "New EHC plans" (:breakdown r))))
                (tc/select-rows (fn [r] (= setting (:setting r))))))))
       :denominator-ds
       send-age-pop-by-la-calendar-year
       :join-keys
       {:left [:calendar-year :geo-code]
        :right [:year :geo-code]}
       :input-fields [:amount :total-pop]
       :output-field :new-ehcps-per-thousand
       :value-fn #(m/approx (dfn/* 1000 (dfn// %1 %2))))
      (tc/drop-columns #":right.*")
      (tc/order-by [:geo-code :calendar-year])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data new-echps-special-total
    :la-name la-name
    :title "New EHCPs in Special Settings"
    :series-name :geo-name
    :x-field :calendar-year
    :x-title "Calendar Year (Jan-Dec)"
    :y-field :new-ehcps-per-thousand
    :y-title "New EHCPs per 1,000"})))


(watermark)
(mc-logo)

;; ---
;;; ## New EHCPs in AP and PRU Settings

^{::clerk/visibility {:code :hide :result :hide}}
(def new-echps-ap_pru-total
  (-> (calculate
       :numerator-ds
       (let [setting :ap_pru_total]
         (newplans/table
          :pipeline-fn
          (fn [ds]
            (-> ds
                (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                newplans/tidy-table
                (tc/select-rows (fn [r] (= "New EHC plans" (:breakdown r))))
                (tc/select-rows (fn [r] (= setting (:setting r))))))))
       :denominator-ds
       send-age-pop-by-la-calendar-year
       :join-keys
       {:left [:calendar-year :geo-code]
        :right [:year :geo-code]}
       :input-fields [:amount :total-pop]
       :output-field :new-ehcps-per-thousand
       :value-fn #(m/approx (dfn/* 1000 (dfn// %1 %2))))
      (tc/drop-columns #":right.*")
      (tc/order-by [:geo-code :calendar-year])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data new-echps-ap_pru-total
    :la-name la-name
    :title "New EHCPs in AP and PRU Settings"
    :series-name :geo-name
    :x-field :calendar-year
    :x-title "Calendar Year (Jan-Dec)"
    :y-field :new-ehcps-per-thousand
    :y-title "New EHCPs per 1,000"})))

(watermark)
(mc-logo)

;; ---
;;; ## New EHCPs in FE Settings

^{::clerk/visibility {:code :hide :result :hide}}
(def new-echps-fe-total
  (-> (calculate
       :numerator-ds
       (let [setting :fe_total]
         (newplans/table
          :pipeline-fn
          (fn [ds]
            (-> ds
                (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
                newplans/tidy-table
                (tc/select-rows (fn [r] (= "New EHC plans" (:breakdown r))))
                (tc/select-rows (fn [r] (= setting (:setting r))))))))
       :denominator-ds
       send-age-pop-by-la-calendar-year
       :join-keys
       {:left [:calendar-year :geo-code]
        :right [:year :geo-code]}
       :input-fields [:amount :total-pop]
       :output-field :new-ehcps-per-thousand
       :value-fn #(m/approx (dfn/* 1000 (dfn// %1 %2))))
      (tc/drop-columns #":right.*")
      (tc/order-by [:geo-code :calendar-year])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data new-echps-fe-total
    :la-name la-name
    :title "New EHCPs in FE Settings"
    :series-name :geo-name
    :x-field :calendar-year
    :x-title "Calendar Year (Jan-Dec)"
    :y-field :new-ehcps-per-thousand
    :y-title "New EHCPs per 1,000"})))

(watermark)
(mc-logo)

;; ---
;;; ## Timeliness - Within 20 weeks (timeliness_20_week.csv)

(watermark)
(mc-logo)

;; ---
;;; ## Timeliness - 20 weeks to 1 year (timeliness_20_week.csv)

(watermark)
(mc-logo)

;; ---
;;; ## Timeliness - Over 1 Year (timeliness_20_week.csv)

(watermark)
(mc-logo)

;; ---
;;; ## Plans Ceaased (ceased_plans.csv)

;; ---
;;; ## Plans at January Census Point (get this from sen_needs_all_plans.csv)

;; ---
;;; ## Plans by Primary Need (get this from sen_needs_all_plans.csv)

;; ---
;;; ## Needs Assessment Requests (requests.csv)

;; ---
;;; ## Needs Assessments Performed (assessments.csv)

;; ---
;;; ## Needs Assessments leading to EHCP issued (assessments.csv)

;; ---
;;; ## Annual Reviews Performed (annual_reviews.csv)

;; ---
;;; ## TODO

;; - Create an Academic Year Population
;; - Create a Jan-Dec Population (not a MYE)
;; - Create a Financial Year Population

