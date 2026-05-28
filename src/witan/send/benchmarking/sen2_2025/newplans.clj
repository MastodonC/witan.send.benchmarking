(ns witan.send.benchmarking.sen2-2025.newplans
  (:require
   [tablecloth.api :as tc]))

;;; FIXME: get the tidying code from benchmarking.quick-stats and make
;;; this look like
;;; witan.send.benchmarking.s251.alleducation-la-regional-national

(def dataset-name "newplans")

(def input-path 
  (format "./src-data/education-health-and-care-plans_2025/data/%s.csv"
          dataset-name))

(def parser-map
  {:time_period                                  :int32
   :time_identifier                              :string
   :geographic_level                             :string
   :country_code                                 :string
   :country_name                                 :string
   :region_code                                  :string
   :region_name                                  :string
   :new_la_code                                  :string
   :old_la_code                                  :string
   :la_name                                      :string
   :breakdown_topic                              :string
   :breakdown                                    :string
   :new_ehc_plans                                [:int32 :relaxed?]
   :mainstream_la_maintained                     [:int32 :relaxed?]
   :mainstream_la_maintained_resourced_provision [:int32 :relaxed?]
   :mainstream_la_maintained_senunit             [:int32 :relaxed?]
   :mainstream_academy                           [:int32 :relaxed?]
   :mainstream_academy_resourced_provision       [:int32 :relaxed?]
   :mainstream_academy_senunit                   [:int32 :relaxed?]
   :mainstream_free_school                       [:int32 :relaxed?]
   :mainstream_free_school_resourced_provision   [:int32 :relaxed?]
   :mainstream_free_school_senunit               [:int32 :relaxed?]
   :mainstream_independent                       [:int32 :relaxed?]
   :mainstream_total                             [:int32 :relaxed?]
   :mainstream_total_pc                          [:float32 :relaxed?]
   :special_la_maintained                        [:int32 :relaxed?]
   :special_academy_free                         [:int32 :relaxed?]
   :special_independent                          [:int32 :relaxed?]
   :special_non_maintained                       [:int32 :relaxed?]
   :special_total                                [:int32 :relaxed?]
   :special_total_pc                             [:float32 :relaxed?]
   :ap_pru_academy                               [:int32 :relaxed?]
   :ap_pru_free_school                           [:int32 :relaxed?]
   :ap_pru_la_maintained                         [:int32 :relaxed?]
   :ap_pru_total                                 [:int32 :relaxed?]
   :AP_PRU_total_pc                              [:float32 :relaxed?]
   :general_fe_tertiary_colleges                 [:int32 :relaxed?]
   :specialist_post_16_institutions              [:int32 :relaxed?]
   :ukrlp_provider                               [:int32 :relaxed?]
   :fe_total                                     [:int32 :relaxed?]
   :fe_total_pc                                  [:float32 :relaxed?]
   :elective_home_education                      [:int32 :relaxed?]
   :other_arrangements_la                        [:int32 :relaxed?]
   :other_arrangements_parents                   [:int32 :relaxed?]
   :online_provider                              [:int32 :relaxed?]
   :w_settings                                   [:int32 :relaxed?]
   :other_schools                                [:int32 :relaxed?]
   :other_placement_settings                     [:int32 :relaxed?]
   :neet                                         [:int32 :relaxed?]
   :neet_ntci                                    [:int32 :relaxed?]
   :neet_other                                   [:int32 :relaxed?]
   :neet_other_csa                               [:int32 :relaxed?]
   :ed_elsewhere                                 [:int32 :relaxed?]
   :ed_elsewhere_pc                              [:float32 :relaxed?]
   :nm_early_years                               [:int32 :relaxed?]
   :nm_early_years_pc                            [:float32 :relaxed?]
   :placement_unknown                            [:int32 :relaxed?]
   :placement_unknown_pc                         [:float32 :relaxed?]
   :await_prov_2022                              [:int32 :relaxed?]
   :perm_ex_2022                                 [:int32 :relaxed?]})

(defn table [& {:keys [input-path pipeline-fn dataset-name parser-fn key-fn]
                :or {input-path input-path
                     dataset-name "sen2-newplans-2025"
                     parser-fn parser-map
                     key-fn keyword}
                :as _opts}]
  (-> input-path
      (tc/dataset 
       {:dataset-name dataset-name
        :key-fn key-fn
        :parser-fn parser-fn})
      (cond-> pipeline-fn pipeline-fn)))

;;; Data Helpers for Other Sources
(defn age->lsrp-age-groups [age-string]
  ({"age 2 and under" "Under 5"
    "age 3"           "Under 5"
    "age 4"           "Under 5"

    "age 5"           "Age 5 to 10"
    "age 6"           "Age 5 to 10"
    "age 7"           "Age 5 to 10"
    "age 8"           "Age 5 to 10"
    "age 9"           "Age 5 to 10"
    "age 10"          "Age 5 to 10"

    "age 11"          "Age 11 to 15"
    "age 12"          "Age 11 to 15"
    "age 13"          "Age 11 to 15"
    "age 14"          "Age 11 to 15"
    "age 15"          "Age 11 to 15"

    "age 16"          "Age 16 to 19"
    "age 17"          "Age 16 to 19"
    "age 18"          "Age 16 to 19"
    "age 19"          "Age 16 to 19"

    "age 20 and over" "Age 20 to 25"} 
   age-string))

(defn tidy-dates [ds]
  (-> ds
      (tc/map-columns :calendar-year [:time_period] identity)
      (tc/drop-columns [:time_period :time_identifier])))

(defn tidy-geocodes [ds]
  (-> ds
      (tc/map-columns :geo-code [:country_code :region_code :new_la_code]  (fn [c r l] (or l r c)))
      (tc/drop-columns [:country_code :region_code :new_la_code :old_la_code])))

(defn tidy-geonames [ds]
  (-> ds
      (tc/map-columns :geo-name [:country_name :region_name :la_name] (fn [c r l] (or l r c)))
      (tc/drop-columns [:country_name :region_name :la_name])))

(def gather-columns
  [:new_ehc_plans
   :mainstream_la_maintained :mainstream_la_maintained_resourced_provision :mainstream_la_maintained_senunit
   :mainstream_academy :mainstream_academy_resourced_provision :mainstream_academy_senunit
   :mainstream_free_school :mainstream_free_school_resourced_provision :mainstream_free_school_senunit
   :mainstream_independent
   :mainstream_total :mainstream_total_pc
   :special_la_maintained :special_academy_free :special_independent :special_non_maintained 
   :special_total :special_total_pc
   :ap_pru_academy :ap_pru_free_school :ap_pru_la_maintained
   :ap_pru_total :AP_PRU_total_pc
   :general_fe_tertiary_colleges :specialist_post_16_institutions :ukrlp_provider
   :fe_total :fe_total_pc
   :elective_home_education :other_arrangements_la :other_arrangements_parents :online_provider
   :w_settings :other_schools :other_placement_settings
   :neet :neet_ntci :neet_other :neet_other_csa
   :ed_elsewhere :ed_elsewhere_pc
   :nm_early_years :nm_early_years_pc
   :placement_unknown :placement_unknown_pc
   :await_prov_2022
   :perm_ex_2022])

(defn tidy-table 
  ([dataset]
   (-> dataset
       tidy-dates
       tidy-geocodes
       tidy-geonames
       (tc/pivot->longer gather-columns {:value-column-name :amount
                                         :target-columns :setting})))
  ([]
   (-> (table)
       tidy-table)))

(comment

  (-> (table)
      tidy-dates
      (tidy-geocodes)
      (tidy-geonames)
      (tc/column-names))

  (table :pipeline-fn tidy-table)

  (into (sorted-set)
        (-> (table :pipeline-fn tidy-table)
            (tc/select-columns [:breakdown_topic :breakdown])
            (tc/unique-by [:breakdown_topic :breakdown])
            (tc/order-by [:breakdown_topic :breakdown])
            (tc/select-rows (fn [r] (= "Age when plan started" (:breakdown_topic r))))
            :breakdown))
  

  )
