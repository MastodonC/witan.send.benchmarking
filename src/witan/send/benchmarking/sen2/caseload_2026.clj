(ns witan.send.benchmarking.sen2.caseload-2026
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.benchmarking.population :as pop]))

;; FIXME: This should be academic-year
(defn time_period->calendar-year [time-period]
  (when time-period (-> time-period str (subs 4) parse-long (+ 2000))))

(def sen2-2026-caseload-file
  (io/as-file
   (io/resource "./education-health-and-care-plans_2026/data/caseload.csv")))

(def table
  (-> sen2-2026-caseload-file
      (tc/dataset {:dataset-name "sen2-2026-caseload" :key-fn keyword
                   :parser-fn {:time_period :int32
                               :time_identifier :string
                               :geographic_level :string
                               :country_code :string
                               :country_name :string
                               :region_code :string
                               :region_name :string
                               :new_la_code :string
                               :old_la_code :int16
                               :la_name :string
                               :breakdown_topic :string
                               :breakdown :string
                               :ehcplans [:int32 :relaxed?]
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
                               :mainstream_total_pc [:int32 :relaxed?]
                               :special_la_maintained [:int32 :relaxed?]
                               :special_academy_free [:int32 :relaxed?]
                               :special_independent [:int32 :relaxed?]
                               :special_non_maintained [:int32 :relaxed?]
                               :special_total [:int32 :relaxed?]
                               :special_total_pc [:int32 :relaxed?]
                               :ap_pru_academy [:int32 :relaxed?]
                               :ap_pru_free_school [:int32 :relaxed?]
                               :ap_pru_la_maintained [:int32 :relaxed?]
                               :ap_pru_total [:int32 :relaxed?]
                               :AP_PRU_total_pc [:int32 :relaxed?]
                               :general_fe_tertiary_colleges [:int32 :relaxed?]
                               :specialist_post_16_institutions [:int32 :relaxed?]
                               :ukrlp_provider [:int32 :relaxed?]
                               :fe_total [:int32 :relaxed?]
                               :fe_total_pc [:int32 :relaxed?]
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
                               :ed_elsewhere_pc [:int32 :relaxed?]
                               :nm_early_years [:int32 :relaxed?]
                               :nm_early_years_pc [:int32 :relaxed?]
                               :placement_unknown [:int32 :relaxed?]
                               :placement_unknown_pc [:int32 :relaxed?]
                               :await_prov_2022 [:int32 :relaxed?]
                               :perm_ex_2022 [:int32 :relaxed?]}})
      (tc/map-columns :calendar-year [:time_period] time_period->calendar-year)
      (delay)))

(comment
  
  @sen2-2026-caseload
  
  (let [cols [:breakdown_topic :breakdown]]
    (-> @sen2-2026-caseload
        (tc/select-columns cols)
        (tc/unique-by cols)
        (tc/order-by cols)
        (tc/head 999)))
  
  )
