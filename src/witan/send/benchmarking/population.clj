(ns witan.send.benchmarking.population
  (:require
   [tablecloth.api :as tc]
   [witan.population.england :as pop]
   [witan.send.benchmarking.age-groupings :as age]
   [tech.v3.dataset.reductions :as dsr]))

(defn sen2-column-names [ds]
  (tc/rename-columns
   ds
   {:ctyua23cd :geo-code
    :ctyua23nm :geo-name
    :year :calendar-year}))

(defn pop-total-by-year [population-by-age year-column population-column]
  (dsr/group-by-column-agg
   [:geo-code :geo-name year-column]
   {population-column (dsr/sum population-column)}
   population-by-age))

(defn pop-total-by-age-group [population-by-age year-column population-column age-column age-group-column]
  (as-> population-by-age $
    (tc/map-columns $ age-group-column [age-column] #(age/age->lsrp-age-group %))
    (dsr/group-by-column-agg
     [:geo-code :geo-name year-column age-group-column]
     {population-column (dsr/sum population-column)}
     $)))

(defn table [& {:keys [population-f
                       min-age
                       max-age
                       la-name-f
                       pipeline-f]
                :or   {population-f pop/->dataset
                       min-age      age/min-send-age
                       max-age      age/max-send-age}}]
  (-> (cond-> {}
        min-age   (assoc :min-age min-age)
        max-age   (assoc :max-age max-age)
        la-name-f (assoc :ctyuanm-f la-name-f))
      population-f
      sen2-column-names
      (cond->
          pipeline-f (pipeline-f))))

(comment

  (table
   :la-name-f #{"Bracknell Forest" "Buckinghamshire" "Cambridgeshire" "Central Bedfordshire" "Hertfordshire" "Oxfordshire" "Surrey" "Trafford" "West Berkshire" "Windsor and Maidenhead" "Wokingham"}
   :pipeline-f
   #(-> %
        (pop-total-by-year :calendar-year :population)
        (tc/order-by [:geo-name :calendar-year])))

  (table
   :la-name-f #{"York"}
   :pipeline-f
   #(-> %
        (tc/order-by [:geo-name :age :calendar-year])))

  (table
   :la-name-f #{"York"}
   :pipeline-f
   #(-> %
        (pop-total-by-year :calendar-year :population)
        (tc/order-by [:geo-name :calendar-year])))


  (table
   :la-name-f #{"York"}
   :pipeline-f
   #(-> %
        (pop-total-by-age-group :calendar-year :population :age :age-group)
        (tc/order-by [:geo-name :calendar-year :age-group-order])))


  )
