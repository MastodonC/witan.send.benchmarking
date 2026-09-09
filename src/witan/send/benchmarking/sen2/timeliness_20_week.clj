(ns witan.send.benchmarking.sen2.timeliness-20-week
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.benchmarking.population :as pop]))

(def file
  (-> "./education-health-and-care-plans_2026/data/timeliness_20_week.csv"
      io/resource
      io/as-file))

(def table
  (-> file
      (tc/dataset
       {:dataset-name "sen2-timelines-20-week-2026"
        :key-fn keyword
        :parser-fn {:time_period :int64
                    :time_identifier :string
                    :geographic_level :string
                    :country_code :string
                    :country_name :string
                    :region_code :string
                    :region_name :string
                    :new_la_code :string
                    :old_la_code :string
                    :la_name :string
                    :breakdown_topic :string
                    :breakdown :string
                    :avg_weeks_issue_plan [:float32 :relaxed?]
                    :avg_weeks_issue_plan_exc [:float32 :relaxed?]
                    :pc_plans_issued_20_weeks_ex [:float32 :relaxed?]
                    :pc_plans_issued_gt_1_year [:float32 :relaxed?]
                    :pc_plans_issued_gt_1_year_ex [:float32 :relaxed?]
                    :pc_plans_issued_gt20weeks_ltyear [:float32 :relaxed?]
                    :pc_plans_issued_gt20weeks_ltyear_ex [:float32 :relaxed?]
                    :pc_plans_issued_within_20_weeks [:float32 :relaxed?]
                    :plans_issued_20_weeks_ex [:int32 :relaxed?]
                    :plans_issued_den [:int32 :relaxed?]
                    :plans_issued_den_ex [:int32 :relaxed?]
                    :plans_issued_gt_1_year [:int32 :relaxed?]
                    :plans_issued_gt_1_year_ex [:int32 :relaxed?]
                    :plans_issued_gt20weeks_ltyear [:int32 :relaxed?]
                    :plans_issued_gt20weeks_ltyear_ex [:int32 :relaxed?]
                    :plans_issued_within_20_weeks [:int32 :relaxed?]}})
      delay))

(comment

  (-> @table
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))
  ;; => sen2-timelines-20-week-2026 [57 2]:
  ;;    
  ;;    |       :breakdown_topic |                                :breakdown |
  ;;    |------------------------|-------------------------------------------|
  ;;    | Age at time of request |                                   Unknown |
  ;;    | Age at time of request |                                    age 10 |
  ;;    | Age at time of request |                                    age 11 |
  ;;    | Age at time of request |                                    age 12 |
  ;;    | Age at time of request |                                    age 13 |
  ;;    | Age at time of request |                                    age 14 |
  ;;    | Age at time of request |                                    age 15 |
  ;;    | Age at time of request |                                    age 16 |
  ;;    | Age at time of request |                                    age 17 |
  ;;    | Age at time of request |                                    age 18 |
  ;;    | Age at time of request |                                    age 19 |
  ;;    | Age at time of request |                           age 2 and under |
  ;;    | Age at time of request |                           age 20 and over |
  ;;    | Age at time of request |                                     age 3 |
  ;;    | Age at time of request |                                     age 4 |
  ;;    | Age at time of request |                                     age 5 |
  ;;    | Age at time of request |                                     age 6 |
  ;;    | Age at time of request |                                     age 7 |
  ;;    | Age at time of request |                                     age 8 |
  ;;    | Age at time of request |                                     age 9 |
  ;;    |          All EHC plans |                             All EHC plans |
  ;;    |              Ethnicity |                    Any other ethnic group |
  ;;    |              Ethnicity |        Asian - Any other Asian background |
  ;;    |              Ethnicity |                       Asian - Bangladeshi |
  ;;    |              Ethnicity |                           Asian - Chinese |
  ;;    |              Ethnicity |                            Asian - Indian |
  ;;    |              Ethnicity |                         Asian - Pakistani |
  ;;    |              Ethnicity |        Black - Any other Black background |
  ;;    |              Ethnicity |                     Black - Black African |
  ;;    |              Ethnicity |                   Black - Black Caribbean |
  ;;    |              Ethnicity |        Mixed - Any other Mixed background |
  ;;    |              Ethnicity |                   Mixed - White and Asian |
  ;;    |              Ethnicity |           Mixed - White and Black African |
  ;;    |              Ethnicity |         Mixed - White and Black Caribbean |
  ;;    |              Ethnicity |                              Unclassified |
  ;;    |              Ethnicity |        White - Any other White background |
  ;;    |              Ethnicity |                        White - Gypsy/Roma |
  ;;    |              Ethnicity |                             White - Irish |
  ;;    |              Ethnicity |       White - Traveller of Irish heritage |
  ;;    |              Ethnicity |                     White - White British |
  ;;    |   Primary type of need |                Autistic spectrum disorder |
  ;;    |   Primary type of need |                             Down Syndrome |
  ;;    |   Primary type of need |                        Hearing impairment |
  ;;    |   Primary type of need |              Moderate learning difficulty |
  ;;    |   Primary type of need |                  Multi-sensory impairment |
  ;;    |   Primary type of need |                              Not reported |
  ;;    |   Primary type of need |            Other difficulty or disability |
  ;;    |   Primary type of need |                       Physical disability |
  ;;    |   Primary type of need | Profound and multiple learning difficulty |
  ;;    |   Primary type of need |                Severe learning difficulty |
  ;;    |   Primary type of need |       Social, emotional and mental health |
  ;;    |   Primary type of need |              Specific learning difficulty |
  ;;    |   Primary type of need |  Speech, language and communication needs |
  ;;    |   Primary type of need |                         Vision impairment |
  ;;    |                    Sex |                                    Female |
  ;;    |                    Sex |                                      Male |
  ;;    |                    Sex |                                   Unknown |
  )
