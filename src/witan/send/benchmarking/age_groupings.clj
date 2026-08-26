(ns witan.send.benchmarking.age-groupings)

(def min-send-age 0)
(def max-send-age 25)

(defn age->financial-phase [^long age]
  (cond
    (#{0 1 2 3} age) :early_years_establishments
    (#{4 5 6 7 8 9 10} age) :primary_schools
    (#{11 12 13 14 15 16} age) :secondary_schools
    (#{17 18 19 20 21 22 23 24 25} age) :post_16))

(def financial-phase-order
  {:early_years_establishments 0
   :primary_schools 1
   :secondary_schools 2
   :post_16 3})

(defn age->lsrp-age-group [^long age]
  (cond
    (#{0 1 2 3 4} age) "Under 5"
    (#{5 6 7 8 9 10} age) "Age 5 to 10"
    (#{11 12 13 14 15} age) "Age 11 to 15"
    (#{16 17 18 19} age) "Age 16 to 19"
    (#{20 21 22 23 24 25} age) "Age 20 to 25"))

(defn sen2-age-group [^long age]
  (age->lsrp-age-group age))


