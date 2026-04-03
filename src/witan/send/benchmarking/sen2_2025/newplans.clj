(ns witan.send.benchmarking.sen2-2025.newplans
  (:require
   [tablecloth.api :as tc]))

(def input-path "./src-data/education-health-and-care-plans_2025/data/newplans.csv")

(def parser-map
  {:time_period :string
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
   :perm_ex_2022 [:int32 :relaxed?]})

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
(defn age->age-group
  "This is handy for converting ages in other datasets to match the age groups here."
  [age]
  (cond 
    (<= age 2) "age 2 and under"
    (<= 20 age 25) "age 20 and over"
    (< 25 age) "Outside of SEND"
    :else (str "age " age)))

;;; Geography
(defn by-geographic-level [ds geographic-level]
  (tc/select-rows ds #(= geographic-level (% :geographic_level))))

(defn national [ds]
  (by-geographic-level ds "National")  )

(defn regional [ds]
  (by-geographic-level ds "Regional"))

(defn local-authority [ds]
  (by-geographic-level ds "Local authority"))

;;; Breakdown Topics
(defn breakdown-topics [& {:keys [ds]
                           :or {ds (table)}}]
  (-> ds
      (tc/select-columns [:breakdown_topic])
      (tc/unique-by [:breakdown_topic])
      (tc/order-by [:breakdown_topic])))

(comment

  (breakdown-topics)
  ;; => sen2-newplans-2025 [4 1]:
  ;;    |      :breakdown_topic |
  ;;    |-----------------------|
  ;;    | Age when plan started |
  ;;    |             Ethnicity |
  ;;    |         New EHC plans |
  ;;    |                   Sex |
  
  )

(defn breakdown-topic
  "Topics available are: Age when plan started, Ethnicity, New EHC plans, Sex"
  [ds & {:keys [breakdown-topic]
         :as _opts}]
  (tc/select-rows ds #(= breakdown-topic (% :breakdown_topic))))

(defn breakdown-topic-age-when-plan-started [ds]
  (breakdown-topic ds {:breakdown-topic "Age when plan started"}))

(defn breakdown-topic-ethnicity [ds]
  (breakdown-topic ds {:breakdown-topic "Ethnicity"}))

(defn breakdown-topic-new-ehc-plans [ds]
  (breakdown-topic ds {:breakdown-topic "New EHC plans"}))

(defn breakdown-topic-sex [ds]
  (breakdown-topic ds {:breakdown-topic "Sex"}))

(comment

  (age->age-group 0)
  ;; => "age 2 and under"

  (age->age-group 2)
  ;; => "age 2 and under"

  (age->age-group 3)
  ;; => "age 3"
  
  (age->age-group 19)
  ;; => "age 19"

  (age->age-group 20)
  ;; => "age 20 and over"
  
  (age->age-group 25)
  ;; => "age 20 and over"

  (age->age-group 26)
  ;; => "Outside of SEND"

  (table 
   :pipeline
   (fn [ds] 
     (-> ds
         (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
         (tc/select-rows #(= "Local authority" (% :geographic_level))))))

  (-> (table)
      local-authority
      breakdown-topic-age-when-plan-started)

  (table 
   :pipeline
   #(-> % local-authority breakdown-topic-age-when-plan-started))

  (table
   :pipeline (comp local-authority breakdown-topic-age-when-plan-started))

  )
