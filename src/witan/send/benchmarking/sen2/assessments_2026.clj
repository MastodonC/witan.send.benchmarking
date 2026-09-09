(ns witan.send.benchmarking.sen2.assessments-2026
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.benchmarking.population :as pop]))

(def file
  (io/as-file
   (io/resource "./education-health-and-care-plans_2026/data/assessments.csv")))

(def table
  (-> file
      (tc/dataset {:dataset-name "sen2-2026-assessments" :key-fn keyword
                   :parser-fn   {:assess_in_year [:int32 :relaxed?]
                                 :assess_issued [:int32 :relaxed?]
                                 :assess_issued_pc [:float32 :relaxed?]
                                 :assess_not_issued [:int32 :relaxed?]
                                 :assess_not_issued_pc [:float32 :relaxed?]
                                 :assess_not_made [:int32 :relaxed?]
                                 :assess_not_made_pc [:float32 :relaxed?]
                                 :assess_withdrawn [:int32 :relaxed?]
                                 :assess_withdrawn_pc [:float32 :relaxed?]
                                 :outcome_decision_within_time [:int32 :relaxed?]
                                 :outcome_decision_over_time [:int32 :relaxed?]
                                 :outcome_decision_within_time_pc [:float32 :relaxed?]
                                 :outcome_decision_over_time_pc [:float32 :relaxed?]
                                 :number_assess_mediation [:int32 :relaxed?]
                                 :number_assess_tribunal [:int32 :relaxed?]
                                 :number_asssess_mediation_tribunal [:int32 :relaxed?]
                                 :number_other_mediation [:int32 :relaxed?]
                                 :number_other_tribunal [:int32 :relaxed?]
                                 :number_other_mediation_tribunal [:int32 :relaxed?]}})
      delay))

(comment

  (tc/info @table)
  ;; => sen2-2026-assessments: descriptive-stats [31 12]:
  ;;    
  ;;    |                          :col-name | :datatype | :n-valid | :n-missing |   :min |         :mean |                      :mode |     :max | :standard-deviation |       :skew |                    :first |           :last |
  ;;    |------------------------------------|-----------|---------:|-----------:|-------:|--------------:|----------------------------|---------:|--------------------:|------------:|---------------------------|-----------------|
  ;;    |                       :time_period |    :int16 |    18970 |          0 | 2019.0 | 2023.88487085 |                            |   2025.0 |          1.04277632 | -1.34241918 |                      2019 |            2025 |
  ;;    |                   :time_identifier |   :string |    18970 |          0 |        |               |              Calendar year |          |                     |             |             Calendar year |   Calendar year |
  ;;    |                  :geographic_level |   :string |    18970 |          0 |        |               |            Local authority |          |                     |             |                  National | Local authority |
  ;;    |                      :country_code |   :string |    18970 |          0 |        |               |                  E92000001 |          |                     |             |                 E92000001 |       E92000001 |
  ;;    |                      :country_name |   :string |    18970 |          0 |        |               |                    England |          |                     |             |                   England |         England |
  ;;    |                       :region_code |   :string |    18838 |        132 |        |               |                  E12000007 |          |                     |             |                           |       E12000009 |
  ;;    |                       :region_name |   :string |    18838 |        132 |        |               |                     London |          |                     |             |                           |      South West |
  ;;    |                       :new_la_code |   :string |    17669 |       1301 |        |               |                            |          |                     |             |                           |       E10000027 |
  ;;    |                       :old_la_code |    :int16 |    17669 |       1301 |  201.0 |  615.60388251 |                            |    943.0 |        279.47489700 | -0.18640180 |                           |             933 |
  ;;    |                           :la_name |   :string |    17669 |       1301 |        |               |                            |          |                     |             |                           |        Somerset |
  ;;    |                   :breakdown_topic |   :string |    18970 |          0 |        |               | Child or young persons age |          |                     |             | All EHC needs assessments |             Sex |
  ;;    |                         :breakdown |   :string |    18970 |          0 |        |               |  All EHC needs assessments |          |                     |             | All EHC needs assessments |            Male |
  ;;    |                    :assess_in_year |    :int32 |    18361 |        609 |    0.0 |  217.82871303 |                            | 118821.0 |       2137.33711829 | 34.76494528 |                           |             472 |
  ;;    |                     :assess_issued |    :int32 |    18846 |        124 |    0.0 |  227.19531996 |                            | 111243.0 |       2128.74132136 | 31.62540743 |                     53899 |             454 |
  ;;    |                  :assess_issued_pc |  :float32 |    17809 |       1161 |    0.0 |   92.85220396 |                            |    100.0 |         13.79959812 | -4.12873936 |                           |           96.20 |
  ;;    |                 :assess_not_issued |    :int32 |    18846 |        124 |    0.0 |   13.89812162 |                            |   7161.0 |        133.96392999 | 31.62250700 |                      3368 |              18 |
  ;;    |              :assess_not_issued_pc |  :float32 |    17809 |       1161 |    0.0 |    6.58890449 |                            |    100.0 |         13.29615369 |  4.33846722 |                           |           3.800 |
  ;;    |                   :assess_not_made |    :int32 |    18200 |        770 |    0.0 |    0.16879121 |                            |    123.0 |          2.76611288 | 28.58507731 |                           |               0 |
  ;;    |                :assess_not_made_pc |  :float32 |    17809 |       1161 |    0.0 |    0.11992251 |                            |    100.0 |          2.10577693 | 34.32107036 |                           |           0.000 |
  ;;    |                  :assess_withdrawn |    :int32 |    18362 |        608 |    0.0 |    0.72639146 |                            |    362.0 |          7.50779197 | 29.21148207 |                           |               0 |
  ;;    |               :assess_withdrawn_pc |  :float32 |    17809 |       1161 |    0.0 |    0.43954181 |                            |    100.0 |          3.29332630 | 18.64531524 |                           |           0.000 |
  ;;    |      :outcome_decision_within_time |    :int32 |    18200 |        770 |    0.0 |    4.29428571 |                            |   2673.0 |         44.05548618 | 36.46460979 |                           |               0 |
  ;;    |        :outcome_decision_over_time |    :int32 |    18200 |        770 |    0.0 |    7.65890110 |                            |   4478.0 |         79.45579588 | 35.53048541 |                           |              18 |
  ;;    |   :outcome_decision_within_time_pc |  :float32 |     9292 |       9678 |    0.0 |   38.97423581 |                            |    100.0 |         36.78641833 |  0.50241321 |                           |           0.000 |
  ;;    |     :outcome_decision_over_time_pc |  :float32 |     9292 |       9678 |    0.0 |   61.02616216 |                            |    100.0 |         36.78649324 | -0.50243160 |                           |           100.0 |
  ;;    |           :number_assess_mediation |    :int32 |    18362 |        608 |    0.0 |    4.16490578 |                            |   2444.0 |         42.76650116 | 35.21342532 |                           |               5 |
  ;;    |            :number_assess_tribunal |    :int32 |    18362 |        608 |    0.0 |    2.88922775 |                            |   1712.0 |         30.38126615 | 32.70478336 |                           |               0 |
  ;;    | :number_asssess_mediation_tribunal |    :int32 |    18200 |        770 |    0.0 |    0.87692308 |                            |    522.0 |          9.28446803 | 35.00271551 |                           |               0 |
  ;;    |            :number_other_mediation |    :int32 |    18362 |        608 |    0.0 |    3.53621610 |                            |   2094.0 |         36.02636450 | 35.34982659 |                           |               8 |
  ;;    |             :number_other_tribunal |    :int32 |    18362 |        608 |    0.0 |    5.92696874 |                            |   3722.0 |         62.54471656 | 34.68617302 |                           |               4 |
  ;;    |   :number_other_mediation_tribunal |    :int32 |    18200 |        770 |    0.0 |    1.08329670 |                            |    701.0 |         11.76350779 | 35.13062622 |                           |               3 |



  
  (-> @table
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))
  ;; => sen2-2026-assessments [43 2]:
  ;;    
  ;;    |           :breakdown_topic |                          :breakdown |
  ;;    |----------------------------|-------------------------------------|
  ;;    |  All EHC needs assessments |           All EHC needs assessments |
  ;;    | Child or young persons age |                         Age unknown |
  ;;    | Child or young persons age |                             Aged 10 |
  ;;    | Child or young persons age |                             Aged 11 |
  ;;    | Child or young persons age |                             Aged 12 |
  ;;    | Child or young persons age |                             Aged 13 |
  ;;    | Child or young persons age |                             Aged 14 |
  ;;    | Child or young persons age |                             Aged 15 |
  ;;    | Child or young persons age |                             Aged 16 |
  ;;    | Child or young persons age |                             Aged 17 |
  ;;    | Child or young persons age |                             Aged 18 |
  ;;    | Child or young persons age |                             Aged 19 |
  ;;    | Child or young persons age |                    Aged 2 and under |
  ;;    | Child or young persons age |                    Aged 20 and over |
  ;;    | Child or young persons age |                              Aged 3 |
  ;;    | Child or young persons age |                              Aged 4 |
  ;;    | Child or young persons age |                              Aged 5 |
  ;;    | Child or young persons age |                              Aged 6 |
  ;;    | Child or young persons age |                              Aged 7 |
  ;;    | Child or young persons age |                              Aged 8 |
  ;;    | Child or young persons age |                              Aged 9 |
  ;;    |                  Ethnicity |              Any other ethnic group |
  ;;    |                  Ethnicity |  Asian - Any other Asian background |
  ;;    |                  Ethnicity |                 Asian - Bangladeshi |
  ;;    |                  Ethnicity |                     Asian - Chinese |
  ;;    |                  Ethnicity |                      Asian - Indian |
  ;;    |                  Ethnicity |                   Asian - Pakistani |
  ;;    |                  Ethnicity |  Black - Any other Black background |
  ;;    |                  Ethnicity |               Black - Black African |
  ;;    |                  Ethnicity |             Black - Black Caribbean |
  ;;    |                  Ethnicity |  Mixed - Any other Mixed background |
  ;;    |                  Ethnicity |             Mixed - White and Asian |
  ;;    |                  Ethnicity |     Mixed - White and Black African |
  ;;    |                  Ethnicity |   Mixed - White and Black Caribbean |
  ;;    |                  Ethnicity |                        Unclassified |
  ;;    |                  Ethnicity |  White - Any other White background |
  ;;    |                  Ethnicity |                  White - Gypsy/Roma |
  ;;    |                  Ethnicity |                       White - Irish |
  ;;    |                  Ethnicity | White - Traveller of Irish heritage |
  ;;    |                  Ethnicity |               White - White British |
  ;;    |                        Sex |                              Female |
  ;;    |                        Sex |                                Male |
  ;;    |                        Sex |                             Unknown |




  )


