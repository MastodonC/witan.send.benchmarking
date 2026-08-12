(ns witan.send.benchmarking.neighbours.statistical
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [tablecloth.api :as tc]))

(def data-url
  (io/resource "lait-2025/sn-model-long.csv"))
(def data
  (delay 
    (-> (io/file data-url)
        (tc/dataset  {:key-fn (fn [k] (-> k s/lower-case keyword))}))))

(defn neighbours
  ([la-name nearest-neighbours]
   (-> nearest-neighbours
       (tc/select-rows #(= la-name (:la_name %)))))
  ([la-name]
   (neighbours la-name @data)))

(defn neighbours-name-pred 
  ([la-name nearest-neighbours]
   (into (sorted-set) (-> (neighbours la-name nearest-neighbours) :sn_name)))
  ([la-name]
   (neighbours-name-pred la-name @data)))
