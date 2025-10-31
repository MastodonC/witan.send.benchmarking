(ns witan.send.benchmarking.sen-need-new-plans-2025
  (:require
   [tablecloth.api :as tc]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.population.england :as pop]
   [fastmath.core :as m]
   [tech.v3.datatype.functional :as dfn]))

(def path "./src-data/education-health-and-care-plans_2025/data/sen_need_newplans.csv")

(def data
  (delay
    (-> path
        (tc/dataset
         {:dataset-name "send_need_newplans"
          :key-fn keyword
          :parser-fn {:time_period :int32
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
                      :ehc_plans [:int32 :relaxed?] :number_asd [:int32 :relaxed?] :number_hi [:int32 :relaxed?] :number_mld [:int32 :relaxed?] :number_msi [:int32 :relaxed?] :number_oth [:int32 :relaxed?] :number_pd [:int32 :relaxed?] :number_pmld [:int32 :relaxed?] :number_semh [:int32 :relaxed?] :number_slcn [:int32 :relaxed?] :number_sld [:int32 :relaxed?] :number_spld [:int32 :relaxed?] :number_vi [:int32 :relaxed?] :number_need_unknown [:int32 :relaxed?] :asd_pc [:float32 :relaxed?] :hi_pc [:float32 :relaxed?] :mld_pc [:float32 :relaxed?] :msi_pc [:float32 :relaxed?] :oth_pc [:float32 :relaxed?] :pd_pc [:float32 :relaxed?] :pmld_pc [:float32 :relaxed?] :semh_pc [:float32 :relaxed?] :slcn_pc [:float32 :relaxed?] :sld_pc [:float32 :relaxed?] :spld_pc [:float32 :relaxed?] :vi_pc [:float32 :relaxed?] :unknown_pc [:float32 :relaxed?]}}))))



(comment

  @data

  (tc/column-names @data)
  (:time_period :time_identifier :geographic_level :country_code :country_name :region_code :region_name :new_la_code :old_la_code :la_name :breakdown_topic :breakdown :ehc_plans )

  (tc/info @data)

  )

(def total-pop-by-la
  (delay 
    (-> (pop/->dataset)
        (as-> $
            (dsr/group-by-column-agg
             [:ctyua23cd :ctyua23nm :year :calendar-year]
             {:total-pop (dsr/sum :population)}
             $))
        (tc/rename-columns {:ctyua23cd :new_la_code})
        (tc/order-by [:new_la_code :calendar-year]))))

(comment 
  (tc/info @total-pop-by-la)
  (tc/info @data)
  )

(def need-new-plans-rate
  (delay
    (-> @data
        (tc/rename-columns {:time_period :year})
        (tc/select-rows #(= "All EHC plans" (:breakdown %)))
        (tc/inner-join @total-pop-by-la
                       [:new_la_code :year])
        (tc/map-columns :rate-asd [:number_asd :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2))))
        (tc/map-columns :rate-hi [:number_hi :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; hi
        (tc/map-columns :rate-mld [:number_mld :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; mld
        (tc/map-columns :rate-msi [:number_msi :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; msi
        (tc/map-columns :rate-oth [:number_oth :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; oth
        (tc/map-columns :rate-pd [:number_pd :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; pd
        (tc/map-columns :rate-pmld [:number_pmld :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; pmld
        (tc/map-columns :rate-semh [:number_semh :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; semh
        (tc/map-columns :rate-slcn [:number_slcn :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; slcn
        (tc/map-columns :rate-sld [:number_sld :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; sld
        (tc/map-columns :rate-spld [:number_spld :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; spld
        (tc/map-columns :rate-vi [:number_vi :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; vi
        (tc/map-columns :rate-need_unknown [:number_need_unknown :total-pop] #(when %1 (dfn/* 10000 (dfn// %1 %2)))) ;; need_unknown
        )))

(defn phase-from-age-label [age]
  (cond
    (#{ "age 2 and under" "age 3"} age) "Early Years"
    (#{ "age 4" "age 5" "age 6" "age 7" "age 8" "age 9" "age 10"} age) "Primary"
    (#{ "age 11" "age 12" "age 13" "age 14" "age 15" "age 16"} age) "Secondary"
    (#{ "age 17" "age 18" "age 19"} age) "Post 16"
    (= "age 20 and over" age) "Post 19"))

(defn fail-to-zero-divide [m d]
  (if (and m d)
    (dfn// m d)
    0))

(def need-new-plans-rate-by-phase
  (delay
    (-> @data
        (tc/rename-columns {:time_period :year})
        (tc/select-rows #(= "Child or young persons age" (:breakdown_topic %)))
        (tc/map-columns :phase [:breakdown] phase-from-age-label)
        (as-> $ 
            (dsr/group-by-column-agg
             [:year :la_name :new_la_code :phase]
             {:ehc_plans           (dsr/sum :ehc_plans)
              :number_asd          (dsr/sum :number_asd)
              :number_hi           (dsr/sum :number_hi)
              :number_mld          (dsr/sum :number_mld)
              :number_msi          (dsr/sum :number_msi)
              :number_oth          (dsr/sum :number_oth)
              :number_pd           (dsr/sum :number_pd)
              :number_pmld         (dsr/sum :number_pmld)
              :number_semh         (dsr/sum :number_semh)
              :number_slcn         (dsr/sum :number_slcn)
              :number_sld          (dsr/sum :number_sld)
              :number_spld         (dsr/sum :number_spld)
              :number_vi           (dsr/sum :number_vi)
              :number_need_unknown (dsr/sum :number_need_unknown)}
             $))
        (tc/map-columns :pc_asd [:number_asd :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_hi [:number_hi :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_mld [:number_mld :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_msi [:number_msi :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_need_unknown [:number_need_unknown :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_oth [:number_oth :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_pd [:number_pd :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_pmld [:number_pmld :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_semh [:number_semh :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_slcn [:number_slcn :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_sld [:number_sld :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_spld [:number_spld :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2))
        (tc/map-columns :pc_vi [:number_vi :ehc_plans] #(m/approx (* 100 (fail-to-zero-divide %1 %2)) 2)))))
