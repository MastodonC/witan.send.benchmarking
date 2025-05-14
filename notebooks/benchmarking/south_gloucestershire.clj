(ns benchmarking.south-gloucestershire
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require [clojure.set :as set]
            [witan.population.england.snpp-2022 :as pop-2022]
            [witan.population.england.snpp-2018 :as pop-2018]
            [witan.send.benchmarking.newplans-2025 :as newplans]
            [witan.send.benchmarking.regional-neighbours :as rn]
            [witan.send.benchmarking.statistical-neighbours :as sn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk-slideshow :as slideshow]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]))

(def la-name "South Gloucestershire")

(clerk/add-viewers! [slideshow/viewer])

(def region (rn/region-name la-name))
(def regional-neighbours (rn/neighbours la-name))
(def region-neighbours-pred (rn/neighbours-name-pred la-name))
(def regional-new-plans-by-age 
  (-> @newplans/new-plans-by-age-by-la
      (tc/select-rows #(region-neighbours-pred (:la_name %)))))

(def statistical-neighbours (sn/neighbours la-name))
(def statistical-neighbours-pred (sn/neighbours-name-pred la-name))
(def statistical-neighbours-new-plans-by-age
  (-> @newplans/new-plans-by-age-by-la
      (tc/select-rows #(statistical-neighbours-pred (:la_name %)))))

(def england-new-plans-by-age (newplans/england-new-plans-by-age))

;;; # South Gloucestershire
(comment
  ;;; witan.send.south-glos/witan.send.south-glos.wp-3-2/notebooks/wp_3_2/wp_3_2_1/
  ;; This has some plotly box plot examples that show all the points as well
  )

(defn plotly-neighbour-comparison
  [la-name age neighbours title new-plans-by-age]
  (let [la-plans (-> new-plans-by-age
                     (tc/select-rows #(#{la-name} (:la_name %)))
                     (tc/select-rows #(= age (:breakdown %))))
        neighbour-plans (-> new-plans-by-age
                            (tc/select-rows #((set neighbours) (:la_name %)))
                            (tc/select-rows #(= age (:breakdown %))))]
    {:data (conj 
            (transduce
             (map (fn [m] (assoc m :time_period (parse-long (:time_period m)))))
             (fn 
               ([] {}) 
               ([acc] (into []
                            (map (fn [[k v]] 
                                   {:x k
                                    :y v
                                    :name k
                                    :marker {:color "orange"}
                                    :type "box"}))
                            acc))
               ([acc x]
                (update-in acc [(:time_period x)] conj (:new-ehcps-per-thousand x))))
             (tc/rows neighbour-plans :as-maps))
            {:x (into [] 
                      (comp 
                       (map parse-long)
                       (map (fn [y] (- (- y (rand 0.2)) 0.1))))
                      (neighbour-plans :time_period))
             :y (into [] (neighbour-plans :new-ehcps-per-thousand))
             :text (into [] (neighbour-plans  :la_name))
             :marker {:color "magenta" :size 6 :symbol "square"}
             :mode "markers"
             :type "scatter"}
            {:x (into [] (map parse-long) (la-plans :time_period))
             :y (into [] (la-plans :new-ehcps-per-thousand))
             :text (into [] (la-plans :la_name))
             :marker {:color "blue" :size 14 :symbol "star-diamond"}
             :mode "markers"
             :type "scatter"})
     :layout {:title {:text title}
              :scattermode "group"
              :scattergap 0.7
              :xaxis {:dtick 1 :title "SEN2 Census Year"}
              :yaxis {:rangemode "tozero" :title "New EHCPs per 1000"}
              :height 400
              :width 500
              :showlegend false}
     :config {:displayModeBar false
              :displayLogo false}}))


{::clerk/visibility {:result :show}}

;; ---
;;; # Regional Neighbours

(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> regional-neighbours
      (tc/select-columns [:la_name])
      (tc/rename-columns {:la_name "LA Name"}))))

;; ---
;;; # Statistical Nearest Neighbours
(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> statistical-neighbours
      (tc/select-columns [:sn :sn_name :sn_prox])
      (tc/rename-columns {:sn_name "Neighbour Name"
                          :sn "Neighbour Rank"
                          :sn_prox "Statistical Proximity"}))))

;; ---
;;; ## Early Years

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 2 and under" statistical-neighbours-pred "Age 2 and Under w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))

 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 3" statistical-neighbours-pred "Age 3 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))

 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 4" statistical-neighbours-pred "Age 4 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## Primary Ages

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 5" statistical-neighbours-pred "Age 5 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 6" statistical-neighbours-pred "Age 6 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 7" statistical-neighbours-pred "Age 7 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 8" statistical-neighbours-pred "Age 8 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 9" statistical-neighbours-pred "Age 9 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 10" statistical-neighbours-pred "Age 10 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la)))


;; ---
;;; ## Secondary Ages
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 11" statistical-neighbours-pred "Age 11 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 12" statistical-neighbours-pred "Age 12 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 13" statistical-neighbours-pred "Age 13 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 14" statistical-neighbours-pred "Age 14 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 15" statistical-neighbours-pred "Age 15 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-neighbour-comparison
   la-name "age 16" statistical-neighbours-pred "Age 16 w/Statistical Neighbours" @newplans/new-plans-by-age-by-la)))




