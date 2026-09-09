(ns witan.send.benchmarking.statistical-neighbours
  (:require
   [clojure.string :as s]
   [tablecloth.api :as tc]))

(def sn-model-long-path "./src-data/lait-2025/sn-model-long.csv")
(def model 
  (-> sn-model-long-path
      (tc/dataset {:key-fn (fn [k] (-> k s/lower-case keyword))})
      delay))


(defn neighbours
  ([la-name nearest-neighbours]
   (-> nearest-neighbours
       (tc/select-rows #(= la-name (:la_name %)))))
  ([la-name]
   (neighbours la-name @model)))

(defn neighbours-name-pred 
  ([la-name nearest-neighbours]
   (into (sorted-set) (-> (neighbours la-name nearest-neighbours) :sn_name)))
  ([la-name]
   (neighbours-name-pred la-name @model)))

(comment

  (tc/info @model)
  ;; => ./src-data/lait-2025/sn-model-long.csv: descriptive-stats [6 12]:
  ;;    | :col-name | :datatype | :n-valid | :n-missing | :min | :mean |           :mode | :max | :standard-deviation | :skew |               :first |           :last |
  ;;    |-----------|-----------|---------:|-----------:|-----:|------:|-----------------|-----:|--------------------:|------:|----------------------|-----------------|
  ;;    |  :la_code |   :string |     1530 |          0 |      |       |       E06000062 |      |                     |       |            E06000001 |       E10000034 |
  ;;    |  :la_name |   :string |     1530 |          0 |      |       |       St Helens |      |                     |       |           Hartlepool |  Worcestershire |
  ;;    |       :sn |    :int16 |     1530 |          0 |  1.0 |   5.5 |                 | 10.0 |          2.87322044 |   0.0 |                    1 |              10 |
  ;;    |  :sn_code |   :string |     1530 |          0 |      |       |       E10000013 |      |                     |       |            E06000003 |       E06000065 |
  ;;    |  :sn_name |   :string |     1530 |          0 |      |       | Gloucestershire |      |                     |       | Redcar and Cleveland | North Yorkshire |
  ;;    |  :sn_prox |   :string |     1530 |          0 |      |       |      Very close |      |                     |       |           Very close |      Very close |
  

  (neighbours "Surrey")
  ;; => ./src-data/lait-2025/sn-model-long.csv [10 6]:
  ;;    |  :la_code | :la_name | :sn |  :sn_code |               :sn_name |   :sn_prox |
  ;;    |-----------|----------|----:|-----------|------------------------|------------|
  ;;    | E10000030 |   Surrey |   1 | E06000040 | Windsor and Maidenhead | Very close |
  ;;    | E10000030 |   Surrey |   2 | E06000060 |        Buckinghamshire | Very close |
  ;;    | E10000030 |   Surrey |   3 | E08000009 |               Trafford | Very close |
  ;;    | E10000030 |   Surrey |   4 | E10000015 |          Hertfordshire | Very close |
  ;;    | E10000030 |   Surrey |   5 | E06000037 |         West Berkshire |      Close |
  ;;    | E10000030 |   Surrey |   6 | E06000056 |   Central Bedfordshire |      Close |
  ;;    | E10000030 |   Surrey |   7 | E06000041 |              Wokingham |      Close |
  ;;    | E10000030 |   Surrey |   8 | E10000025 |            Oxfordshire |      Close |
  ;;    | E10000030 |   Surrey |   9 | E06000036 |       Bracknell Forest |      Close |
  ;;    | E10000030 |   Surrey |  10 | E10000003 |         Cambridgeshire |      Close |

  (neighbours-name-pred "Surrey")
  ;; => #{"Bracknell Forest"
  ;;      "Buckinghamshire"
  ;;      "Cambridgeshire"
  ;;      "Central Bedfordshire"
  ;;      "Hertfordshire"
  ;;      "Oxfordshire"
  ;;      "Trafford"
  ;;      "West Berkshire"
  ;;      "Windsor and Maidenhead"
  ;;      "Wokingham"}

  )
