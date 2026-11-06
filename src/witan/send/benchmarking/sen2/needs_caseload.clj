(ns witan.send.benchmarking.sen2.needs-caseload
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]

;; FIXME: This should be academic-year
(defn time_period->calendar-year [time-period]
  (when time-period (-> time-period str (subs 4) parse-long (+ 2000))))

(defn time_period->academic-year [time-period]
  (when time-period
    (let [time-period' (str time-period)]
      (format "%s/%s" (subs time-period' 0 4) (subs time-period' 4)))))

(comment

  (time_period->calendar-year 202627)
  ;; => 2027

  (time_period->academic-year 202627)
  ;; => "2026/27"
  )

(def file
  (io/as-file
   (io/resource "./education-health-and-care-plans_2026/data/sen_needs_caseload.csv")))

(def table
  (-> file
      (tc/dataset {:dataset-name "sen2-2026-needs-caseload" :key-fn keyword
                   :parser-fn 
                   {:time_period :int32
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
                               :ehc_plans           [:int32 :relaxed?]
                               :number_asd          [:int32 :relaxed?]
                               :number_ds           [:int32 :relaxed?]
                               :number_hi           [:int32 :relaxed?]
                               :number_mld          [:int32 :relaxed?]
                               :number_msi          [:int32 :relaxed?]
                               :number_oth          [:int32 :relaxed?]
                               :number_pd           [:int32 :relaxed?]
                               :number_pmld         [:int32 :relaxed?]
                               :number_semh         [:int32 :relaxed?]
                               :number_slcn         [:int32 :relaxed?]
                               :number_sld          [:int32 :relaxed?]
                               :number_spld         [:int32 :relaxed?]
                               :number_vi           [:int32 :relaxed?]
                               :number_need_unknown [:int32 :relaxed?]
                               :asd_pc              [:float32 :relaxed?]
                               :ds_pc               [:float32 :relaxed?]
                               :hi_pc               [:float32 :relaxed?]
                               :mld_pc              [:float32 :relaxed?]
                               :msi_pc              [:float32 :relaxed?]
                               :oth_pc              [:float32 :relaxed?]
                               :pd_pc               [:float32 :relaxed?]
                               :pmld_pc             [:float32 :relaxed?]
                               :semh_pc             [:float32 :relaxed?]
                               :slcn_pc             [:float32 :relaxed?]
                               :sld_pc              [:float32 :relaxed?]
                               :spld_pc             [:float32 :relaxed?]
                               :vi_pc               [:float32 :relaxed?]
                               :unknown_pc          [:float32 :relaxed?]}})
      (tc/map-columns :calendar-year [:time_period] time_period->calendar-year)
      (delay)))

(comment
  
   @table

  (let [cols [:breakdown_topic :breakdown]]
    (-> @table
        (tc/select-columns cols)
        (tc/unique-by cols)
        (tc/order-by cols)
        (tc/head 999))))
