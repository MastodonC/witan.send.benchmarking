^{:clay {:title "My statistical neighbours"}}
(ns benchmarking.my-neighbours
  (:require
   [clojure.string :as s]
   [tablecloth.api :as tc]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.neighbours.statistical :as sn]))

(defn load-csv [path]
  (tc/dataset path {:key-fn (fn [k] (-> k
                                        s/lower-case
                                        (s/replace " " "_")
                                        keyword))}))

(def background (load-csv "resources/my-data/background-variables.csv"))
(def variables  (load-csv "resources/my-data/variables.csv"))

(defn standardise [col]
  (dfn// (dfn/- col (dfn/mean col))
         (dfn/standard-deviation col)))

(comment
  (tc/column-names background)
  (tc/column-names variables)
  (dfn/mean (:ures_over3_eng_per background))
  (dfn/standard-deviation (:ures_over3_eng_per background))
  (standardise (:ures_over3_eng_per background))
  (dfn/mean (standardise (:ures_over3_eng_per background)))
  (compare-las "Camden" "Islington")
  (-> (compare-las "Camden" "Islington")
      (tc/order-by :weight :desc)
      (tc/head 10)
      (tc/drop-columns [:contribution])
      )
   
  (descriptions :ures_over3_eng_per)
  )

(def weights
  (zipmap (map (comp keyword s/lower-case) (:variable_code variables))
          (:weight variables)))

(def standardised
  (-> background
      tc/drop-missing
      (tc/update-columns (keys weights) standardise)))

(defn la-row [ds la-name]
  (first (tc/rows (tc/select-rows ds #(= la-name (:la_name %))) :as-maps)))

(def descriptions
  (zipmap (map (comp keyword s/lower-case) (:variable_code variables))
          (:description variables)))

(defn compare-las [la-a la-b]
  (let [za (la-row standardised la-a)   ; z-values, for the maths
        zb (la-row standardised la-b)
        ra (la-row background la-a)     ; raw percentages, for reading
        rb (la-row background la-b)]
    (-> (for [[col w] weights]
          (let [d (- (get za col) (get zb col))] 
            {:variable     (descriptions col)
             :weight       w
             la-a          (get ra col)
             la-b          (get rb col)
             :contribution (* w d d)}))
        tc/dataset
        (tc/order-by :contribution))))