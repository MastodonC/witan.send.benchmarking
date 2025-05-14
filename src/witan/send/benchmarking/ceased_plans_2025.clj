(ns witan.send.benchmarking.ceased-plans-2025
  (:require
   [tablecloth.api :as tc]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.caseload-2025 :as caseload]))

(def sen2-2025-ceased-plans-filename
  "./src-data/education-health-and-care-plans_2025/data/ceased_plans.csv")
(def ceased-plans
  (delay
    (-> sen2-2025-ceased-plans-filename
        (tc/dataset {:dataset-name "sen2-2025-ceased-plans" :key-fn keyword}))))

(def ceased-plans-by-age-by-la
  (delay
    (-> @ceased-plans
        #_(tc/map-columns :time_period [:time_period] #(str % "/" (inc (- % 2000))))
        (tc/map-columns :time_period [:time_period] inc)
        (tc/select-rows #(#{"Local authority"} (:geographic_level %)))
        (tc/inner-join
         (-> @caseload/sen2-2025-caseload-all-ehcps-by-age
             (tc/drop-missing [:new_la_code])
             #_(tc/map-columns :time_period [:time_period] {202324 "2023/24" 202425 "2024/25"})
             (tc/map-columns :time_period [:time_period] {202324 2024 202425 2025})
             (tc/map-columns
              :breakdown [:breakdown]
              (fn [age]
                (cond
                  (#{"under 3"} age) "age 2 and under"
                  (#{"Age unknown" "age 20" "age 21" "age 22" "age 23" "age 24" "age 25"} age) "age 20 and over"
                  :else age)))
             (tc/group-by [:time_period :new_la_code :breakdown])
             (tc/aggregate {:ehcplans #(dfn/sum (:ehcplans %))}))
         [:time_period :new_la_code :breakdown])
        (tc/replace-missing :ehcplans :value 0)
        (tc/map-columns :ceased-ehcps-per-1000-ehcps :float64
                        [:total_ceased :ehcplans]
                        #(float 
                          (if (or (nil? %1) (nil? %2) (zero? %1) (zero? %2))
                            0.0
                            (* 1000 (/ %1 %2))))))))



(comment

  @sen2-2025-ceased-plans

  (tc/info @sen2-2025-ceased-plans)
  ;; => sen2-2025-ceased-plans: descriptive-stats [23 12]:
  ;;    |          :col-name | :datatype | :n-valid | :n-missing |   :min |         :mean |                :mode |    :max | :standard-deviation |       :skew |               :first |           :last |
  ;;    |--------------------|-----------|---------:|-----------:|-------:|--------------:|----------------------|--------:|--------------------:|------------:|----------------------|-----------------|
  ;;    |       :time_period |    :int16 |    21164 |          0 | 2022.0 | 2023.50552826 |                      |  2024.0 |          0.51506406 | -0.19016789 |                 2022 |            2024 |
  ;;    |   :time_identifier |   :string |    21164 |          0 |        |               |        Calendar year |         |                     |             |        Calendar year |   Calendar year |
  ;;    |  :geographic_level |   :string |    21164 |          0 |        |               |      Local authority |         |                     |             |             National | Local authority |
  ;;    |      :country_code |   :string |    21164 |          0 |        |               |            E92000001 |         |                     |             |            E92000001 |       E92000001 |
  ;;    |      :country_name |   :string |    21164 |          0 |        |               |              England |         |                     |             |              England |         England |
  ;;    |       :region_code |   :string |    20983 |        181 |        |               |            E12000007 |         |                     |             |                      |       E12000009 |
  ;;    |       :region_name |   :string |    20983 |        181 |        |               |               London |         |                     |             |                      |      South West |
  ;;    |       :new_la_code |   :string |    19429 |       1735 |        |               |                      |         |                     |             |                      |       E10000027 |
  ;;    |       :old_la_code |    :int16 |    19429 |       1735 |  201.0 |  612.55463482 |                      |   943.0 |        279.57904594 | -0.16694852 |                      |             933 |
  ;;    |           :la_name |   :string |    19429 |       1735 |        |               |                      |         |                     |             |                      |        Somerset |
  ;;    |   :breakdown_topic |   :string |    21164 |          0 |        |               |    Type of placement |         |                     |             | All ceased EHC plans | Years plan held |
  ;;    |         :breakdown |   :string |    21164 |          0 |        |               | All ceased EHC plans |         |                     |             | All ceased EHC plans |    Under a year |
  ;;    |           :max_age |    :int16 |    21164 |          0 |    0.0 |    4.25165375 |                      |  2141.0 |         43.52835242 | 30.77452606 |                 2141 |               0 |
  ;;    |         :needs_met |    :int16 |    21164 |          0 |    0.0 |    7.74962200 |                      |  4054.0 |         72.25169503 | 32.98932704 |                 4054 |               0 |
  ;;    |                :he |    :int16 |    21164 |          0 |    0.0 |    4.64812890 |                      |  2579.0 |         41.94285371 | 31.51229698 |                 1454 |               0 |
  ;;    |            :employ |    :int16 |    21164 |          0 |    0.0 |    7.74168399 |                      |  4053.0 |         73.35911233 | 32.24935748 |                 2734 |               0 |
  ;;    |          :transfer |    :int16 |    21164 |          0 |    0.0 |   26.16679267 |                      | 13953.0 |        215.41868601 | 36.18164789 |                 9400 |               6 |
  ;;    |         :no_engage |    :int16 |    21164 |          0 |    0.0 |   32.42378567 |                      | 16091.0 |        306.69467980 | 31.57074164 |                11734 |               0 |
  ;;    | :moved_outside_eng |    :int16 |    21164 |          0 |    0.0 |    1.81468531 |                      |   928.0 |         15.05716430 | 33.69865677 |                  490 |               0 |
  ;;    |          :deceased |    :int16 |    21164 |          0 |    0.0 |    0.97637498 |                      |   519.0 |          8.46560144 | 36.84608583 |                  519 |               0 |
  ;;    |           :not_rec |    :int16 |    21164 |          0 |    0.0 |    0.00070875 |                      |     5.0 |          0.04908527 | 77.87593497 |                    5 |               0 |
  ;;    |             :other |    :int16 |    21164 |          0 |    0.0 |    2.27395577 |                      |  2193.0 |         28.52639174 | 35.57120861 |                 2193 |               0 |
  ;;    |      :total_ceased |    :int32 |    21164 |          0 |    1.0 |   88.05334530 |                      | 44862.0 |        760.51047024 | 34.51875094 |                34724 |               6 |

  (into (sorted-set) (-> @sen2-2025-ceased-plans :breakdown_topic))
  #{"Age plan ceased" "All ceased EHC plans" "Ethnicity" "Sex" "Type of placement" "Years plan held"}

  (into (sorted-set)
        (-> @sen2-2025-ceased-plans
            (tc/select-rows #(#{"Age plan ceased"} (% :breakdown_topic)))
            :breakdown))
  #{"Age unknown" "age 10" "age 11" "age 12" "age 13" "age 14" "age 15" "age 16" "age 17" "age 18" "age 19" "age 2 and under" "age 20 and over" "age 3" "age 4" "age 5" "age 6" "age 7" "age 8" "age 9"}

  @ceased-plans-by-age-by-la

  (tc/info @ceased-plans-by-age-by-la)


  (-> @ceased-plans-by-age-by-la
      (tc/select-columns [:la_name :total_ceased :ehcplans :ceased-ehcps-per-1000-ehcps])
      (tc/convert-types {:ceased-ehcps-per-1000-ehcps :float64})
      (tc/info))


  )


