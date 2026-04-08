(ns benchmarking.financial-benchmarking
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require
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
   [witan.send.population.england :as pop])
  (:import
   (java.time LocalDateTime)
   (java.time.format DateTimeFormatter)))

(def la-name "South Gloucestershire")
(def stat-neighbours (sn/neighbours la-name))
(def statistical-neighbours-pred (sn/neighbours-name-pred la-name))
(def la-and-stat-neighbours-pred (conj statistical-neighbours-pred la-name))

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

(def mc-logo-url "https://www.mastodonc.com/wp-content/themes/MastodonC-2018/dist/images/logo_mastodonc.png")

(defn mc-logo []
  (clerk/html
   {::clerk/width :full}
   [:div.fixed.bottom-0.right-12 [:img {:src mc-logo-url}]]))

(defn watermark []
  (clerk/html
   {::clerk/width :full}
   [:div.fixed.bottom-0.left-12 [:p.font-sans.italic la-name]]))


;; ---
;;; # Benchmarking metrics that would be useful:

;; - Per capita spend on top-ups by Schools sector i.e. Early Years, Primary, secondary, Post 16
;; -  Per capita spend on Place funding by above same sectors
;; -  Per capita spend on voluntary/Private /independent sectors
;; -  Per capita Spend on Alternative Provision
;; -  Per capita Spend on SEND (all SEND)
;; -  Add top ups to place payments?
;; -  Per capita SEND related Income received from ICB
;;
;; I suspect they won't be easy to generate but can you please try.



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
    (#{0 1 2 3} age) "Early Years"
    (#{4 5 6 7 8 9 10} age) "Primary"
    (#{11 12 13 14 15 16} age) "Secondary"
    (#{17 18 19} age) "Post 16"
    (= 20 age) "Post 19"))

;; ---
;; DECISION: We're going to use 0-25 population as our denominator for
;; benchmarking comparison of SEND Spending
(def send-age-pop-by-la-calendar-year
  (-> (pop/->dataset)
      (as-> $
          (dsr/group-by-column-agg
           [:ctyua23cd :ctyua23nm :year :calendar-year]
           {:total-pop (dsr/sum :population)}
           $))
      (tc/rename-columns {:ctyua23cd :geo-code
                          :ctyua23nm :geo-name})
      (tc/order-by [:geo-code :calendar-year])))

;; So, the 2024/25 Financial year covers April, May, June, July,
;; August, September, October, November, December in 2024, which is 9
;; months and January, February, March in 2025 which is 3 months, so
;; the population for the 2024/25 Financial year is:
;;
;; (+ (* (/ 9 12) pop-2024) (* (/ 3 12) pop-2025))
(defn year-to-financial-year [year]
  (format "FY %d/%d" year (-> year (rem 100) inc)))

(defn total-pop-by-la-financial-year [population-by-cy]
  (-> (tc/inner-join
       (-> population-by-cy
           (tc/map-columns :y1-pop [:total-pop] #(* (/ 9 12) %))
           (tc/map-columns :financial-year [:calendar-year]
                           year-to-financial-year))
       (-> population-by-cy
           (tc/map-columns :y2-pop [:total-pop] #(* (/ 3 12) %))
           (tc/map-columns :financial-year [:calendar-year]
                           #(-> % dec year-to-financial-year)))
       [:geo-code :financial-year])
      (tc/select-columns [:geo-code :geo-name
                          #_:calendar-year #_:right.calendar-year
                          :financial-year #_:right.financial-year
                          #_:total-pop #_:right.total-pop
                          :y1-pop :y2-pop])
      (tc/map-columns :financial-year-pop [:y1-pop :y2-pop] #(int (+ %1 %2)))))


(def send-age-pop-by-la-per-financial-year
  (-> (total-pop-by-la-financial-year send-age-pop-by-la-calendar-year)
      (tc/select-rows #(la-and-stat-neighbours-pred (:geo-name %)))))

(
;;; ## Calculation Helper
 )

(defn calculate [& {:keys [numerator-ds
                           denominator-ds
                           join-keys
                           input-fields
                           value-fn
                           output-field]
                    :or {value-fn #(m/approx (dfn// %1 %2))}}]
  (-> numerator-ds
      (tc/inner-join denominator-ds join-keys)
      (tc/map-columns output-field input-fields value-fn)))

(
;;; Deck
 )
{::clerk/visibility {:result :show}}


^{::clerk/visibility {:code :hide :result :hide}}
(comment

;;; # Section 251 Sandbox

  (s251/table
   :pipeline-fn
   (fn [ds]
     (-> ds
         (tc/select-rows (fn [r] (= "1.0.2 High needs place funding within Individual Schools Budget"
                                    (:category_of_expenditure r))))
         (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
         #_(s251/tidy-table)
         #_(tc/map-columns :time_period [:time_period] s251/format-financial-year)
         #_(tc/select-rows (fn [r] (= :gross_expenditure (:setting r))))

         (tc/drop-columns [:time_identifier :geographic_level :country_code :country_name :region_code :region_name :EES_order])
         (tc/head 500))))

  )

;; ---
;;; # High Needs Amount per CYP of SEND Age (0-25)

;; ---
;;; ## Total Place Funding for Special Schools and AP/PRUs
;;
;; Source: Section 251 (2024/2025), Line 1.0.2,
;; sen_and_special_schools and pupil_referral_units_and_alt_provision
;; columns.
^{::clerk/visibility {:code :hide :result :hide}}
(def total-funding-for-schools-and-ap-prus
  (-> (calculate
       :numerator-ds
       (s251/table
        :pipeline-fn
        (fn [ds]
          (-> ds
              (tc/select-rows (fn [r] (#{"1.0.2 High needs place funding within Individual Schools Budget"}
                                       (:category_of_expenditure r))))
              (tc/select-rows (fn [r] (la-and-stat-neighbours-pred (:la_name r))))
              (s251/tidy-table)
              (tc/map-columns :time_period [:time_period] s251/format-financial-year)
              (tc/select-rows (fn [r]
                                (#{:sen_and_special_schools :pupil_referral_units_and_alt_provision}
                                 (:setting r))))
              (as-> $
                  (dsr/group-by-column-agg
                   [:time_period :geo-code :geo-name]
                   {:amount (dsr/sum :amount)}
                   $)))))
       :denominator-ds
       send-age-pop-by-la-per-financial-year
       :join-keys
       {:left [:time_period :geo-code]
        :right [:financial-year :geo-code]}
       :input-fields [:amount :financial-year-pop]
       :output-field :net-expenditure-per-send-age-cyp)
      (tc/drop-columns #":inner.*")
      (tc/drop-columns [:financial-year :time_identifier :geographic_level])))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data total-funding-for-schools-and-ap-prus
    :la-name la-name
    :title "Total Place Funding for Special Schools and AP/PRUs"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Net Expenditure per SEND age CYP (£s)"})))

;; ---
;;; ## Top up funding (maintained schools, academies, free schools and colleges)
;;
;; Source: Section 251 (2024/2025), Lines 1.2.1, 1.2.2, 1.2.4, 1.2.11 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def top-up-funding-maintained-schools
  (-> (calculate
       :numerator-ds
       (-> (s251/table
            :pipeline-fn
            (fn [ds]
              (-> ds
                  (tc/select-rows (fn [r]

                                    ((some-fn
                                      (fn [s] (re-find #"^1.2.1 " s))
                                      (fn [s] (re-find #"^1.2.2 " s))
                                      (fn [s] (re-find #"^1.2.4 " s))
                                      (fn [s] (re-find #"^1.2.11 " s)))
                                     (:category_of_expenditure r))))
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
   {:neighbour-data top-up-funding-maintained-schools
    :la-name la-name
    :title "Top up funding (maintained schools, academies, free schools and colleges)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

;; ---
;;; ## Top up funding (non-maintained and independent schools and colleges)
;;
;; Source: Source: Section 251 (2024/2025), Line 1.2.3 using gross_expenditure column
^{::clerk/visibility {:code :hide :result :hide}}
(def top-up-funding-non-maintained-and-independent-schools-and-colleges
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
   {:neighbour-data top-up-funding-non-maintained-and-independent-schools-and-colleges
    :la-name la-name
    :title "Top up funding (non-maintained and independent schools and colleges)"
    :series-name :geo-name
    :x-field :time_period
    :x-title "Financial Year"
    :y-field :net-expenditure-per-send-age-cyp
    :y-title "Gross Expenditure per SEND age CYP (£s)"})))

