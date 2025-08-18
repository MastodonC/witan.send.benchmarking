(ns witan.send.benchmarking.assessment-2025
  (:require
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.population.england.snpp-2022 :as pop]))

(def sen2-2025-assessments-filename
  "./src-data/education-health-and-care-plans_2025/data/assessments.csv")
(def sen2-2025-assessments
  (delay 
    (-> sen2-2025-assessments-filename
        (tc/dataset {:dataset-name "sen2-2025-assessments" :key-fn keyword
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
                                   :number_other_mediation_tribunal [:int32 :relaxed?]}}))))

(comment

  (tc/info @sen2-2025-assessments)
  ;; => sen2-2025-assessments: descriptive-stats [31 12]:
  ;;    |                          :col-name | :datatype | :n-valid | :n-missing |   :min |         :mean |                      :mode |     :max | :standard-deviation |       :skew |                    :first |           :last |
  ;;    |------------------------------------|-----------|---------:|-----------:|-------:|--------------:|----------------------------|---------:|--------------------:|------------:|---------------------------|-----------------|
  ;;    |                       :time_period |    :int16 |    12847 |          0 | 2019.0 | 2023.35338990 |                            |   2024.0 |          0.85467502 | -2.55293545 |                      2019 |            2024 |
  ;;    |                   :time_identifier |   :string |    12847 |          0 |        |               |              Calendar year |          |                     |             |             Calendar year |   Calendar year |
  ;;    |                  :geographic_level |   :string |    12847 |          0 |        |               |            Local authority |          |                     |             |                  National | Local authority |
  ;;    |                      :country_code |   :string |    12847 |          0 |        |               |                  E92000001 |          |                     |             |                 E92000001 |       E92000001 |
  ;;    |                      :country_name |   :string |    12847 |          0 |        |               |                    England |          |                     |             |                   England |         England |
  ;;    |                       :region_code |   :string |    12757 |         90 |        |               |                  E12000007 |          |                     |             |                           |       E12000009 |
  ;;    |                       :region_name |   :string |    12757 |         90 |        |               |                     London |          |                     |             |                           |      South West |
  ;;    |                       :new_la_code |   :string |    11964 |        883 |        |               |                            |          |                     |             |                           |       E10000027 |
  ;;    |                       :old_la_code |    :int16 |    11964 |        883 |  201.0 |  615.34612170 |                            |    943.0 |        279.41992667 | -0.18496889 |                           |             933 |
  ;;    |                           :la_name |   :string |    11964 |        883 |        |               |                            |          |                     |             |                           |        Somerset |
  ;;    |                   :breakdown_topic |   :string |    12847 |          0 |        |               | Child or young persons age |          |                     |             | All EHC needs assessments |             Sex |
  ;;    |                         :breakdown |   :string |    12847 |          0 |        |               |  All EHC needs assessments |          |                     |             | All EHC needs assessments |            Male |
  ;;    |                    :assess_in_year |    :int32 |    12267 |        580 |    0.0 |  209.80981495 |                            | 105340.0 |       2042.62289775 | 33.75772240 |                           |             502 |
  ;;    |                     :assess_issued |    :int32 |    12752 |         95 |    0.0 |  231.08869197 |                            |  98547.0 |       2109.55547285 | 29.50509886 |                     53899 |             484 |
  ;;    |                  :assess_issued_pc |  :float32 |    11900 |        947 |    0.0 |   93.08670591 |                            |    100.0 |         13.69701587 | -4.15034428 |                           |           96.40 |
  ;;    |                 :assess_not_issued |    :int32 |    12752 |         95 |    0.0 |   13.80112923 |                            |   6404.0 |        129.84627883 | 29.48635265 |                      3368 |              18 |
  ;;    |              :assess_not_issued_pc |  :float32 |    11900 |        947 |    0.0 |    6.29509244 |                            |    100.0 |         13.10650042 |  4.39474138 |                           |           3.600 |
  ;;    |                   :assess_not_made |    :int32 |    12106 |        741 |    0.0 |    0.18338014 |                            |    123.0 |          3.00660015 | 28.86211015 |                           |               0 |
  ;;    |                :assess_not_made_pc |  :float32 |    11900 |        947 |    0.0 |    0.13867227 |                            |    100.0 |          2.29374129 | 31.89319538 |                           |           0.000 |
  ;;    |                  :assess_withdrawn |    :int32 |    12268 |        579 |    0.0 |    0.74877731 |                            |    362.0 |          7.57934434 | 29.65824954 |                           |               0 |
  ;;    |               :assess_withdrawn_pc |  :float32 |    11900 |        947 |    0.0 |    0.47982353 |                            |    100.0 |          3.41894076 | 17.30431749 |                           |           0.000 |
  ;;    |      :outcome_decision_within_time |    :int32 |    12106 |        741 |    0.0 |    3.80637700 |                            |   2179.0 |         38.66107739 | 35.58901611 |                           |               6 |
  ;;    |        :outcome_decision_over_time |    :int32 |    12106 |        741 |    0.0 |    7.07549975 |                            |   4212.0 |         74.09217999 | 35.59853374 |                           |              12 |
  ;;    |   :outcome_decision_within_time_pc |  :float32 |     5902 |       6945 |    0.0 |   39.20269391 |                            |    100.0 |         37.13429811 |  0.49427932 |                           |           33.30 |
  ;;    |     :outcome_decision_over_time_pc |  :float32 |     5902 |       6945 |    0.0 |   60.79762780 |                            |    100.0 |         37.13437055 | -0.49429610 |                           |           66.70 |
  ;;    |           :number_assess_mediation |    :int32 |    12268 |        579 |    0.0 |    3.84316922 |                            |   2124.0 |         39.01795447 | 34.12954851 |                           |               3 |
  ;;    |            :number_assess_tribunal |    :int32 |    12268 |        579 |    0.0 |    2.64982067 |                            |   1445.0 |         27.47676880 | 31.76742346 |                           |               3 |
  ;;    | :number_asssess_mediation_tribunal |    :int32 |    12106 |        741 |    0.0 |    0.80092516 |                            |    475.0 |          8.52916482 | 34.60409156 |                           |               1 |
  ;;    |            :number_other_mediation |    :int32 |    12268 |        579 |    0.0 |    3.24453864 |                            |   1862.0 |         32.85945177 | 34.47611163 |                           |              13 |
  ;;    |             :number_other_tribunal |    :int32 |    12268 |        579 |    0.0 |    5.23043691 |                            |   3088.0 |         54.89218224 | 33.38515373 |                           |               6 |
  ;;    |   :number_other_mediation_tribunal |    :int32 |    12106 |        741 |    0.0 |    1.03485875 |                            |    701.0 |         11.54302438 | 36.40161529 |                           |               1 |


  ;; I used this to sketch out a first pass of the parser-fn
  (into 
   []
   (map
    (fn [{:keys [col-name datatype]}]
      [col-name [:int32 :relaxed?]]))
   (-> (tc/info sen2-2025-assessments)
       (tc/select-columns [:col-name :datatype])
       (tc/rows :as-maps)))

  (-> sen2-2025-assessments
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))
  ;; => sen2-2025-assessments [43 2]:
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
