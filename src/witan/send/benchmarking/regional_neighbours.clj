(ns witan.send.benchmarking.regional-neighbours
  (:require
   [clojure.string :as s]
   [tablecloth.api :as tc]))

(def la-lookup-path "./src-data/lait-2025/la-lookup.csv")
(def lookup 
  (-> la-lookup-path
      (tc/dataset {:key-fn (fn [k] (-> k s/lower-case keyword))})
      delay))

(defn region-name [la-name]
  (-> @lookup
      (tc/select-rows #(= la-name (:la_name %)))
      (tc/rows :as-maps)
      first
      :region_name))

(defn neighbours
  ([la-name geographic-neighbours]
   (-> geographic-neighbours
       (tc/select-rows #(= (region-name la-name) (:region_name %)))
       (tc/drop-rows #(= la-name (:la_name %)))))
  ([la-name]
   (neighbours la-name @lookup)))

(defn neighbours-name-pred 
  ([la-name geographic-neighbours]
   (into (sorted-set) (-> (neighbours la-name geographic-neighbours) :la_name)))
  ([la-name]
   (neighbours-name-pred la-name @lookup)))

(comment
  (tc/info @lookup)
  ;; => ./src-data/lait-2025/la-lookup.csv: descriptive-stats [5 12]:
  ;;    |    :col-name | :datatype | :n-valid | :n-missing |  :min |        :mean |     :mode |  :max | :standard-deviation |       :skew |               :first |                    :last |
  ;;    |--------------|-----------|---------:|-----------:|------:|-------------:|-----------|------:|--------------------:|------------:|----------------------|--------------------------|
  ;;    |     :la_code |   :string |      153 |          0 |       |              | E06000062 |       |                     |             |            E09000002 |                E06000014 |
  ;;    | :old_la_code |    :int16 |      153 |          0 | 201.0 | 616.74509804 |           | 943.0 |        279.89921893 | -0.20194348 |                  301 |                      816 |
  ;;    |     :la_name |   :string |      153 |          0 |       |              | St Helens |       |                     |             | Barking and Dagenham |                     York |
  ;;    | :region_code |   :string |      153 |          0 |       |              | E12000007 |       |                     |             |            E12000007 |                E12000003 |
  ;;    | :region_name |   :string |      153 |          0 |       |              |    London |       |                     |             |               London | Yorkshire and The Humber |

  (-> @lookup
      (tc/select-columns [:region_code :region_name])
      (tc/unique-by [:region_code :region_name])
      (tc/order-by [:region_code :region_name]))
  ;; => ./src-data/lait-2025/la-lookup.csv [9 2]:
  ;;    | :region_code |             :region_name |
  ;;    |--------------|--------------------------|
  ;;    |    E12000001 |               North East |
  ;;    |    E12000002 |               North West |
  ;;    |    E12000003 | Yorkshire and The Humber |
  ;;    |    E12000004 |            East Midlands |
  ;;    |    E12000005 |            West Midlands |
  ;;    |    E12000006 |          East of England |
  ;;    |    E12000007 |                   London |
  ;;    |    E12000008 |               South East |
  ;;    |    E12000009 |               South West |

  (-> @lookup
      (tc/select-rows #(= "Yorkshire and The Humber" (:region_name %))))
  ;; => ./src-data/lait-2025/la-lookup.csv [15 5]:
  ;;    |  :la_code | :old_la_code |                    :la_name | :region_code |             :region_name |
  ;;    |-----------|-------------:|-----------------------------|--------------|--------------------------|
  ;;    | E08000016 |          370 |                    Barnsley |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000032 |          380 |                    Bradford |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000033 |          381 |                  Calderdale |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000017 |          371 |                   Doncaster |    E12000003 | Yorkshire and The Humber |
  ;;    | E06000011 |          811 |    East Riding of Yorkshire |    E12000003 | Yorkshire and The Humber |
  ;;    | E06000010 |          810 | Kingston upon Hull, City of |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000034 |          382 |                    Kirklees |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000035 |          383 |                       Leeds |    E12000003 | Yorkshire and The Humber |
  ;;    | E06000012 |          812 |     North East Lincolnshire |    E12000003 | Yorkshire and The Humber |
  ;;    | E06000013 |          813 |          North Lincolnshire |    E12000003 | Yorkshire and The Humber |
  ;;    | E06000065 |          815 |             North Yorkshire |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000018 |          372 |                   Rotherham |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000019 |          373 |                   Sheffield |    E12000003 | Yorkshire and The Humber |
  ;;    | E08000036 |          384 |                   Wakefield |    E12000003 | Yorkshire and The Humber |
  ;;    | E06000014 |          816 |                        York |    E12000003 | Yorkshire and The Humber |


  (region-name "Surrey")
  ;; => "South East"

  (neighbours "Surrey")
  ;; => ./src-data/lait-2025/la-lookup.csv [18 5]:
  ;;    |  :la_code | :old_la_code |               :la_name | :region_code | :region_name |
  ;;    |-----------|-------------:|------------------------|--------------|--------------|
  ;;    | E06000036 |          867 |       Bracknell Forest |    E12000008 |   South East |
  ;;    | E06000043 |          846 |      Brighton and Hove |    E12000008 |   South East |
  ;;    | E06000060 |          825 |        Buckinghamshire |    E12000008 |   South East |
  ;;    | E10000011 |          845 |            East Sussex |    E12000008 |   South East |
  ;;    | E10000014 |          850 |              Hampshire |    E12000008 |   South East |
  ;;    | E06000046 |          921 |          Isle of Wight |    E12000008 |   South East |
  ;;    | E10000016 |          886 |                   Kent |    E12000008 |   South East |
  ;;    | E06000035 |          887 |                 Medway |    E12000008 |   South East |
  ;;    | E06000042 |          826 |          Milton Keynes |    E12000008 |   South East |
  ;;    | E10000025 |          931 |            Oxfordshire |    E12000008 |   South East |
  ;;    | E06000044 |          851 |             Portsmouth |    E12000008 |   South East |
  ;;    | E06000038 |          870 |                Reading |    E12000008 |   South East |
  ;;    | E06000039 |          871 |                 Slough |    E12000008 |   South East |
  ;;    | E06000045 |          852 |            Southampton |    E12000008 |   South East |
  ;;    | E06000037 |          869 |         West Berkshire |    E12000008 |   South East |
  ;;    | E10000032 |          938 |            West Sussex |    E12000008 |   South East |
  ;;    | E06000040 |          868 | Windsor and Maidenhead |    E12000008 |   South East |
  ;;    | E06000041 |          872 |              Wokingham |    E12000008 |   South East |

  (neighbours-name-pred "Surrey")
  ;; => #{"Bracknell Forest"
  ;;      "Brighton and Hove"
  ;;      "Buckinghamshire"
  ;;      "East Sussex"
  ;;      "Hampshire"
  ;;      "Isle of Wight"
  ;;      "Kent"
  ;;      "Medway"
  ;;      "Milton Keynes"
  ;;      "Oxfordshire"
  ;;      "Portsmouth"
  ;;      "Reading"
  ;;      "Slough"
  ;;      "Southampton"
  ;;      "West Berkshire"
  ;;      "West Sussex"
  ;;      "Windsor and Maidenhead"
  ;;      "Wokingham"}

  

  )
