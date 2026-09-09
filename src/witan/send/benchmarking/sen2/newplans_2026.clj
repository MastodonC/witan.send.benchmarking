(ns witan.send.benchmarking.sen2.newplans-2026
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.benchmarking.population :as pop]))

(def file
  (-> "./education-health-and-care-plans_2026/data/newplans.csv"
      io/resource
      io/as-file))

(def table
  (-> file
      (tc/dataset 
       {:dataset-name "sen2-newplans-2026"
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
                    :new_ehc_plans [:int32 :relaxed?]
                    :mainstream_la_maintained [:int32 :relaxed?]
                    :mainstream_la_maintained_resourced_provision [:int32 :relaxed?]
                    :mainstream_la_maintained_senunit [:int32 :relaxed?]
                    :mainstream_academy [:int32 :relaxed?]
                    :mainstream_academy_resourced_provision [:int32 :relaxed?]
                    :mainstream_academy_senunit [:int32 :relaxed?]
                    :mainstream_free_school [:int32 :relaxed?]
                    :mainstream_free_school_resourced_provision [:int32 :relaxed?]
                    :mainstream_free_school_senunit [:int32 :relaxed?]
                    :mainstream_independent [:int32 :relaxed?]
                    :mainstream_total [:int32 :relaxed?]
                    :mainstream_total_pc [:float32 :relaxed?]
                    :special_la_maintained [:int32 :relaxed?]
                    :special_academy_free [:int32 :relaxed?]
                    :special_independent [:int32 :relaxed?]
                    :special_non_maintained [:int32 :relaxed?]
                    :special_total [:int32 :relaxed?]
                    :special_total_pc [:float32 :relaxed?]
                    :ap_pru_academy [:int32 :relaxed?]
                    :ap_pru_free_school [:int32 :relaxed?]
                    :ap_pru_la_maintained [:int32 :relaxed?]
                    :ap_pru_total [:int32 :relaxed?]
                    :AP_PRU_total_pc [:float32 :relaxed?]
                    :general_fe_tertiary_colleges [:int32 :relaxed?]
                    :specialist_post_16_institutions [:int32 :relaxed?]
                    :ukrlp_provider [:int32 :relaxed?]
                    :fe_total [:int32 :relaxed?]
                    :fe_total_pc [:float32 :relaxed?]
                    :elective_home_education [:int32 :relaxed?]
                    :other_arrangements_la [:int32 :relaxed?]
                    :other_arrangements_parents [:int32 :relaxed?]
                    :online_provider [:int32 :relaxed?]
                    :w_settings [:int32 :relaxed?]
                    :other_schools [:int32 :relaxed?]
                    :other_placement_settings [:int32 :relaxed?]
                    :neet [:int32 :relaxed?]
                    :neet_ntci [:int32 :relaxed?]
                    :neet_other [:int32 :relaxed?]
                    :neet_other_csa [:int32 :relaxed?]
                    :ed_elsewhere [:int32 :relaxed?]
                    :ed_elsewhere_pc [:float32 :relaxed?]
                    :nm_early_years [:int32 :relaxed?]
                    :nm_early_years_pc [:float32 :relaxed?]
                    :placement_unknown [:int32 :relaxed?]
                    :placement_unknown_pc [:float32 :relaxed?]
                    :await_prov_2022 [:int32 :relaxed?]
                    :perm_ex_2022 [:int32 :relaxed?]}})
      delay))

(comment

  (-> @table
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))

  ;; => sen2-newplans-2026 [42 2]:
  ;;    
  ;;    |      :breakdown_topic |                          :breakdown |
  ;;    |-----------------------|-------------------------------------|
  ;;    | Age when plan started |                              Age 10 |
  ;;    | Age when plan started |                              Age 11 |
  ;;    | Age when plan started |                              Age 12 |
  ;;    | Age when plan started |                              Age 13 |
  ;;    | Age when plan started |                              Age 14 |
  ;;    | Age when plan started |                              Age 15 |
  ;;    | Age when plan started |                              Age 16 |
  ;;    | Age when plan started |                              Age 17 |
  ;;    | Age when plan started |                              Age 18 |
  ;;    | Age when plan started |                              Age 19 |
  ;;    | Age when plan started |                     Age 2 and under |
  ;;    | Age when plan started |                     Age 20 and over |
  ;;    | Age when plan started |                               Age 3 |
  ;;    | Age when plan started |                               Age 4 |
  ;;    | Age when plan started |                               Age 5 |
  ;;    | Age when plan started |                               Age 6 |
  ;;    | Age when plan started |                               Age 8 |
  ;;    | Age when plan started |                               Age 9 |
  ;;    | Age when plan started |                               age 7 |
  ;;    |     All new EHC plans |                   All new EHC plans |
  ;;    |             Ethnicity |              Any other ethnic group |
  ;;    |             Ethnicity |  Asian - Any other Asian background |
  ;;    |             Ethnicity |                 Asian - Bangladeshi |
  ;;    |             Ethnicity |                     Asian - Chinese |
  ;;    |             Ethnicity |                      Asian - Indian |
  ;;    |             Ethnicity |                   Asian - Pakistani |
  ;;    |             Ethnicity |  Black - Any other Black background |
  ;;    |             Ethnicity |               Black - Black African |
  ;;    |             Ethnicity |             Black - Black Caribbean |
  ;;    |             Ethnicity |  Mixed - Any other Mixed background |
  ;;    |             Ethnicity |             Mixed - White and Asian |
  ;;    |             Ethnicity |     Mixed - White and Black African |
  ;;    |             Ethnicity |   Mixed - White and Black Caribbean |
  ;;    |             Ethnicity |                        Unclassified |
  ;;    |             Ethnicity |  White - Any other White background |
  ;;    |             Ethnicity |                  White - Gypsy/Roma |
  ;;    |             Ethnicity |                       White - Irish |
  ;;    |             Ethnicity | White - Traveller of Irish heritage |
  ;;    |             Ethnicity |               White - White British |
  ;;    |                   Sex |                              Female |
  ;;    |                   Sex |                                Male |
  ;;    |                   Sex |                             Unknown |


  )
