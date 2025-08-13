(ns witan.send.benchmarking.newplans-2025
  (:require
   [tablecloth.api :as tc]
   [tech.v3.dataset.reductions :as dsr]
   [witan.population.england.snpp-2022 :as pop-2022]
   [witan.send.benchmarking.newplans-2025 :as newplans]))

(def newplans-path "./src-data/education-health-and-care-plans_2025/data/newplans.csv")

(def newplans
  (-> newplans-path
      (tc/dataset 
       {:dataset-name "sen2-newplans-2025"
        :key-fn keyword
        :parser-fn {:time_period :string
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

  @newplans

  (tc/info @newplans)

  (-> @newplans
      :mainstream_la_maintained
      meta)
  
  (-> @newplans
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))
  ;; => sen2-newplans-2025 [42 2]:
  ;;    |      :breakdown_topic |                          :breakdown |
  ;;    |-----------------------|-------------------------------------|
  ;;    | Age when plan started |                              age 10 |
  ;;    | Age when plan started |                              age 11 |
  ;;    | Age when plan started |                              age 12 |
  ;;    | Age when plan started |                              age 13 |
  ;;    | Age when plan started |                              age 14 |
  ;;    | Age when plan started |                              age 15 |
  ;;    | Age when plan started |                              age 16 |
  ;;    | Age when plan started |                              age 17 |
  ;;    | Age when plan started |                              age 18 |
  ;;    | Age when plan started |                              age 19 |
  ;;    | Age when plan started |                     age 2 and under |
  ;;    | Age when plan started |                     age 20 and over |
  ;;    | Age when plan started |                               age 3 |
  ;;    | Age when plan started |                               age 4 |
  ;;    | Age when plan started |                               age 5 |
  ;;    | Age when plan started |                               age 6 |
  ;;    | Age when plan started |                               age 7 |
  ;;    | Age when plan started |                               age 8 |
  ;;    | Age when plan started |                               age 9 |
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
  ;;    |         New EHC plans |                       New EHC plans |
  ;;    |                   Sex |                              Female |
  ;;    |                   Sex |                                Male |
  ;;    |                   Sex |                             Unknown |

  )

(def snpp-2025-by-age
  (delay
    (-> (pop-2022/->witan-send-population)
        (tc/map-columns
         :age-group [:age]
         (fn [age]
           (cond 
             (<= age 2) 2               ; 2 and under
             (<= 20 age) 20             ; 20 and over
             :else age)))
        (tc/map-columns
         :age-group-label [:age-group]
         (fn [age-group]
           (cond 
             (<= age-group 2) "age 2 and under"
             (<= 20 age-group) "age 20 and over"
             :else (str "age " age-group))))
        (as-> $ (dsr/group-by-column-agg 
                 [:UTLA22CD :UTLA22NM :snpp-year :calendar-year :age-group :age-group-label]
                 {:population (dsr/sum :population)}
                 $)))))

(def snpp-2025-send-age-pop
  (delay
    (dsr/group-by-column-agg
     [:UTLA22CD :UTLA22NM :snpp-year :calendar-year]
     {:population (dsr/sum :population)}
     (pop-2022/->witan-send-population))))

(def new-plans-by-la
  (delay
    (-> @newplans
        (tc/drop-missing [:la_name])
        (tc/select-rows #(= "New EHC plans" (:breakdown %)))
        (tc/update-columns {:time_period (partial map parse-long)})
        (tc/inner-join @snpp-2025-send-age-pop
                       {:left [:new_la_code :time_period]
                        :right [:UTLA22CD :snpp-year]})
        (tc/map-columns 
         :new-ehcps-per-thousand
         [:new_ehc_plans :population]
         (fn [new-plans pop] (* 1000 (/ new-plans pop)))))))

(def new-plans-by-age-by-la
  (delay
    (-> @newplans
        (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
        (tc/select-rows #(= "Local authority" (% :geographic_level)))
        (tc/inner-join 
         (-> @snpp-2025-by-age
             (tc/map-columns :snpp-year [:snpp-year] str))
         {:left [:new_la_code :time_period :breakdown]
          :right [:UTLA22CD :snpp-year :age-group-label]})
        (tc/map-columns 
         :new-ehcps-per-thousand
         [:new_ehc_plans :population]
         (fn [new-plans pop] (* 1000 (/ new-plans pop)))))))


#_
(defn england-new-plans-by-age []
  (-> @newplans
      (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
      (tc/select-rows #(= "National" (% :geographic_level)))))


#_
(defn regional-new-plans-by-age [region-name]
  (-> @newplans
      (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
      (tc/select-rows #(= "Regional" (% :geographic_level)))
      (tc/select-rows #(= region-name (% :region_name)))))


#_
(defn regional-neigbours-new-plans-by-age [region-name]
  (-> @newplans
      (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
      (tc/select-rows #(= "Local authority" (% :geographic_level)))
      (tc/select-rows #(= region-name (% :region_name)))))

#_
(defn la-name->region-name [la-name]
  (-> @newplans 
      (tc/select-rows #(= la-name (% :la_name)))
      :region_name
      first))


#_
(defn la-regional-neigbours-new-plans-by-age [la-name]
  (let [region-name (la-name->region-name la-name)]
    (-> @newplans
        (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
        (tc/select-rows #(= "Local authority" (% :geographic_level)))
        (tc/select-rows #(= region-name (% :region_name))))))


#_
(defn la-new-plans-by-age [la-name]
  (-> @newplans
      (tc/select-rows #(= "Age when plan started" (% :breakdown_topic)))
      (tc/select-rows #(= "Local authority" (% :geographic_level)))
      (tc/select-rows #(= la-name (% :la_name)))))

(comment 

  (into (sorted-set) (@newplans :geographic_level))
  #{"Local authority" "National" "Regional"}

  (into (sorted-set) (@newplans :region_name))
  #{nil "East Midlands" "East of England" "London" "North East" "North West" "South East" "South West" "West Midlands" "Yorkshire and The Humber"}
  
  (regional-new-plans-by-age "East of England")
  (regional-neigbours-new-plans-by-age "East of England")
  (england-new-plans-by-age)
  (la-new-plans-by-age "Surrey")
  (la-regional-neigbours-new-plans-by-age "Surrey")
  (la-name->region-name "Surrey")

  (into (sorted-set) (@newplans :la_name))

  (let [la-name "York"]
    (-> @newplans
        (tc/select-rows #(= la-name (:la_name %)))
        (tc/select-rows #(= "New EHC plans" (:breakdown %)))
        #_(tc/select-columns [:time_period :new_ehc_plans])
        (tc/convert-types {:time_period :int16})
        ))

  )

