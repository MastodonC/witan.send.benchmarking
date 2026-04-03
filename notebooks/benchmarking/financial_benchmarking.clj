(ns benchmarking.financial-benchmarking
  (:require 
   [fastmath.core :as m]
   [tablecloth.api :as tc]
   [tech.v3.dataset.reductions :as dsr]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.s251.alleducation-la-regional-national :as s251]
   [witan.send.population.england :as pop]))

;; Benchmarking metrics that would be useful:

;; Per capita spend on top-ups by Schools sector i.e. Early Years, Primary, secondary, Post 16
;; Per capita spend on Place funding by above same sectors
;; Per capita spend on voluntary/Private /independent sectors
;; Per capita Spend on Alternative Provision
;; Per capita Spend on SEND (all SEND)
;; Per capita SEND related Income received from ICB

;; I suspect they won't be easy to generate but can you please try.

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
  (format "%d%d" year (-> year (rem 100) inc)))

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

(comment 
  (-> (total-pop-by-la-financial-year send-age-pop-by-la-calendar-year)
      (tc/order-by :geo-code :financial-year)
      (tc/head 500))
  )

(def net-expenditure-per-send-age-cyp
  (-> (s251/table
       :pipeline-fn
       (fn [ds]
         (-> ds
             (tc/select-rows (fn [r] (= "1.0.2 High needs place funding within Individual Schools Budget"
                                        (:category_of_expenditure r))))
             (tc/select-rows (fn [r] (= "Southampton" (:la_name r))))
             (s251/tidy-table)
             (tc/select-rows (fn [r] (= :net_expenditure (:setting r))))
             (tc/rename-columns {:time_period :financial-year}))))
      (tc/inner-join 
       (-> (total-pop-by-la-financial-year send-age-pop-by-la-calendar-year)
           #_(tc/select-rows (fn [r] (= "Southampton" (:geo-name r)))))
       [:geo-code :financial-year])
      (tc/map-columns :net-expenditure-per-send-age-cyp
                      [:amount :financial-year-pop]
                      #(m/approx (dfn// %1 %2)))))

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

(calculate
 :numerator-ds
 (s251/table
  :pipeline-fn
  (fn [ds]
    (-> ds
        (tc/select-rows (fn [r] (= "1.0.2 High needs place funding within Individual Schools Budget"
                                   (:category_of_expenditure r))))
        (tc/select-rows (fn [r] (= "Southampton" (:la_name r))))
        (s251/tidy-table)
        (tc/select-rows (fn [r] (= :net_expenditure (:setting r)))))))
 :denominator-ds
 (total-pop-by-la-financial-year send-age-pop-by-la-calendar-year)
 :join-keys
 {:left [:time_period :geo-code]
  :right [:financial-year :geo-code]}
 :input-fields [:amount :financial-year-pop]
 :output-field :net-expenditure-per-send-age-cyp)
