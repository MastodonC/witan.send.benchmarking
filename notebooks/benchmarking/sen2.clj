^:kindly/hide-code
(ns benchmarking.sen2
  (:require [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [witan.send.benchmarking.population :as population]
            [witan.send.benchmarking.neighbours.statistical :as sn]))

;;; # SEN2 Benchmarking

;;; ## LA and Neighbours
^:kindly/hide-code
(def la-name "Barking and Dagenham")

^:kindly/hide-code
(def neighbours (sn/neighbours la-name))
^:kindly/hide-code
(def neighbours-pred (sn/neighbours-name-pred la-name))

;;; ### Statistical Neighbours
^:kindly/hide-code
(-> neighbours
    (tc/select-columns [:sn :sn_name :sn_prox])
    (tc/rename-columns {:sn "Rank" :sn_name "Name" :sn_prox "Proximity"}))

;;; ## Total Population
^:kindly/hide-code
(def population-by-age
  (population/table
   :la-name-f (conj neighbours-pred la-name)
   :pipeline-f
   #(-> %
        (tc/order-by [:geo-name :age :calendar-year]))))

^:kindly/hide-code
(def population-total
  (-> (population/pop-total-by-year population-by-age :calendar-year :population)
      (tc/order-by [:geo-name :calendar-year])))

^:kindly/hide-code
(-> population-total
    (tc/select-rows #(= (:geo-name %) la-name))
    (tc/rename-columns {:calendar-year "Calendar Year" :population "0-25 Population"})
    (pj/lay-line "Calendar Year" "0-25 Population" {:color :geo-name})
    (pj/lay-point "Calendar Year" "0-25 Population" {:color :geo-name :shape :geo-name})
    (pj/options {:title "0-25 Population" :title-font-size 26 :tooltip true}))

;; (pj/config)

^:kindly/hide-code
(-> population-total
    (tc/drop-rows #(= (:geo-name %) la-name))
    (tc/rename-columns {:calendar-year "Calendar Year" :population "0-25 Population" :geo-name "Local Authority"})
    (pj/lay-boxplot "Calendar Year" "0-25 Population" {:x-type :categorical :color "orange"})
    ;; (pj/lay-point {:data (-> population-total
    ;;                          (tc/select-rows #(= (:geo-name %) la-name))
    ;;                          (tc/rename-columns {:calendar-year "Calendar Year" :population "0-25 Population" :geo-name "Local Authority"}))
    ;;                ;; :alpha 0.3 
    ;;                ;; :color "blue" :shape :circle
    ;;                :x-type :categorical})
    (pj/options {:height 600 :width 1600}))

^:kindly/hide-code
(def population-by-age-group
  (-> (population/pop-total-by-age-group population-by-age :calendar-year :population :age :age-group)
      (tc/order-by [:geo-name :calendar-year :age-group-order])))

^:kindly/hide-code
(-> population-by-age-group
    (tc/select-rows #(= (:geo-name %) la-name))
    (tc/rename-columns {:calendar-year "Calendar Year" :population "0-25 Population" :age-group "Age Group"})
    (pj/lay-line "Calendar Year" "0-25 Population" {:color "Age Group"})
    (pj/lay-point {:shape "Age Group" :color "Age Group"})
    (pj/options {:title "0-25 Population"}))
