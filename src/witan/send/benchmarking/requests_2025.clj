(ns witan.send.benchmarking.requests-2025
  (:require
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.population.england.snpp-2022 :as pop]))

(def sen2-2025-requests-filename
  "./src-data/education-health-and-care-plans_2025/data/requests.csv")
(def sen2-2025-requests
  (delay
    (-> sen2-2025-requests-filename
        (tc/dataset {:dataset-name "sen2-2025-requests" :key-fn keyword
                     :parser-fn {:requests_received_in_year [:int32 :relaxed?]
                                 :requests_rya [:int32 :relaxed?]
                                 :requests_decided_to_assess [:int32 :relaxed?]
                                 :requests_decided_not_to_assess [:int32 :relaxed?]
                                 :requests_decision_not_made [:int32 :relaxed?]
                                 :requests_withdrawn [:int32 :relaxed?]
                                 :request_assess_pc [:float32 :relaxed?]
                                 :request_not_assess_pc [:float32 :relaxed?]
                                 :request_outstanding_pc [:float32 :relaxed?]
                                 :request_withdrawn_pc [:float32 :relaxed?]
                                 :request_outcome_six_weeks [:int32 :relaxed?]
                                 :request_outcome_over_6_weeks [:int32 :relaxed?]
                                 :request_outcome_six_weeks_pc [:float32 :relaxed?]
                                 :request_outcome_over_6_weeks_pc [:float32 :relaxed?]
                                 :mediation_related_request [:int32 :relaxed?]
                                 :tribunal_related_request [:int32 :relaxed?]
                                 :tribunal_after_mediation_request [:int32 :relaxed?]}}))))

(comment

  (tc/info @sen2-2025-requests)
  ;; => sen2-2025-requests: descriptive-stats [29 12]:
  ;;    |                         :col-name | :datatype | :n-valid | :n-missing |   :min |         :mean |                                  :mode |     :max | :standard-deviation |       :skew |                                 :first |           :last |
  ;;    |-----------------------------------|-----------|---------:|-----------:|-------:|--------------:|----------------------------------------|---------:|--------------------:|------------:|----------------------------------------|-----------------|
  ;;    |                      :time_period |    :int16 |    13169 |          0 | 2019.0 | 2023.35629129 |                                        |   2024.0 |          0.84796059 | -2.55621199 |                                   2019 |            2024 |
  ;;    |                  :time_identifier |   :string |    13169 |          0 |        |               |                          Calendar year |          |                     |             |                          Calendar year |   Calendar year |
  ;;    |                 :geographic_level |   :string |    13169 |          0 |        |               |                        Local authority |          |                     |             |                               National | Local authority |
  ;;    |                     :country_code |   :string |    13169 |          0 |        |               |                              E92000001 |          |                     |             |                              E92000001 |       E92000001 |
  ;;    |                     :country_name |   :string |    13169 |          0 |        |               |                                England |          |                     |             |                                England |         England |
  ;;    |                      :region_code |   :string |    13079 |         90 |        |               |                              E12000007 |          |                     |             |                                        |       E12000009 |
  ;;    |                      :region_name |   :string |    13079 |         90 |        |               |                                 London |          |                     |             |                                        |      South West |
  ;;    |                      :new_la_code |   :string |    12285 |        884 |        |               |                                        |          |                     |             |                                        |       E10000027 |
  ;;    |                      :old_la_code |    :int16 |    12285 |        884 |  201.0 |  616.00724461 |                                        |    943.0 |        279.34582900 | -0.19095181 |                                        |             933 |
  ;;    |                          :la_name |   :string |    12285 |        884 |        |               |                                        |          |                     |             |                                        |        Somerset |
  ;;    |                  :breakdown_topic |   :string |    13169 |          0 |        |               |             Child or young persons age |          |                     |             | All requests for EHC needs assessments |             Sex |
  ;;    |                        :breakdown |   :string |    13169 |          0 |        |               | All requests for EHC needs assessments |          |                     |             | All requests for EHC needs assessments |            Male |
  ;;    |        :requests_received_in_year |    :int32 |    13061 |        108 |    0.0 |  352.74665033 |                                        | 154489.0 |       3246.70140462 | 30.17093054 |                                  82329 |             772 |
  ;;    |                     :requests_rya |    :int32 |    13061 |        108 |    0.0 |    0.60546666 |                                        |    501.0 |         11.60994441 | 30.82060355 |                                     16 |               0 |
  ;;    |       :requests_decided_to_assess |    :int32 |    12415 |        754 |    0.0 |  187.33370922 |                                        | 101045.0 |       1873.65472948 | 36.15643459 |                                        |             442 |
  ;;    |   :requests_decided_not_to_assess |    :int32 |    13061 |        108 |    0.0 |   84.78079779 |                                        |  38889.0 |        785.23864865 | 30.37741825 |                                  18755 |             289 |
  ;;    |       :requests_decision_not_made |    :int32 |    12415 |        754 |    0.0 |   21.36995570 |                                        |  12038.0 |        212.97203901 | 36.33111867 |                                        |              27 |
  ;;    |               :requests_withdrawn |    :int32 |    12415 |        754 |    0.0 |    4.38115183 |                                        |   2517.0 |         47.41899420 | 31.51182252 |                                        |              14 |
  ;;    |                :request_assess_pc |  :float32 |    12096 |       1073 |    0.0 |   65.93854154 |                                        |    100.0 |         22.41166759 | -0.83139702 |                                        |           57.30 |
  ;;    |            :request_not_assess_pc |  :float32 |    12096 |       1073 |    0.0 |   24.82187497 |                                        |    100.0 |         21.23279312 |  1.28077712 |                                        |           37.40 |
  ;;    |           :request_outstanding_pc |  :float32 |    12096 |       1073 |    0.0 |    7.73782243 |                                        |    100.0 |         11.84268141 |  3.95780043 |                                        |           3.500 |
  ;;    |             :request_withdrawn_pc |  :float32 |    12096 |       1073 |    0.0 |    1.50214121 |                                        |    100.0 |          6.30630932 |  8.58872299 |                                        |           1.800 |
  ;;    |        :request_outcome_six_weeks |    :int32 |    12415 |        754 |    0.0 |  229.67990334 |                                        | 126160.0 |       2305.65420430 | 36.18470144 |                                        |             677 |
  ;;    |     :request_outcome_over_6_weeks |    :int32 |    12415 |        754 |    0.0 |   41.02875554 |                                        |  22593.0 |        412.54860845 | 35.58203521 |                                        |              20 |
  ;;    |     :request_outcome_six_weeks_pc |  :float32 |    12093 |       1076 |    0.0 |   83.92941369 |                                        |    100.0 |         21.77387332 | -1.91643417 |                                        |           97.10 |
  ;;    |  :request_outcome_over_6_weeks_pc |  :float32 |    12093 |       1076 |    0.0 |   15.61031176 |                                        |    100.0 |         21.09487801 |  1.90402309 |                                        |           2.900 |
  ;;    |        :mediation_related_request |    :int32 |    12415 |        754 |    0.0 |   17.50495369 |                                        |  10507.0 |        181.54114987 | 36.71565010 |                                        |             117 |
  ;;    |         :tribunal_related_request |    :int32 |    12415 |        754 |    0.0 |    7.95038260 |                                        |   4655.0 |         83.34328349 | 34.66974470 |                                        |              58 |
  ;;    | :tribunal_after_mediation_request |    :int32 |    12415 |        754 |    0.0 |    2.39871124 |                                        |   1473.0 |         25.33719177 | 35.87664422 |                                        |              22 |

  ;; I used this to sketch out a first pass of the parser-fn
  (into 
   []
   (map
    (fn [{:keys [col-name datatype]}]
      [col-name [:int32 :relaxed?]]))
   (-> (tc/info @sen2-2025-requests)
       (tc/select-columns [:col-name :datatype])
       (tc/rows :as-maps)))
  




  (-> @sen2-2025-requests
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))
  ;; => Execution error (IllegalArgumentException) at tech.v3.dataset.base/columns (base.clj:75).
  ;;    Don't know how to create ISeq from: clojure.lang.Delay


  )


