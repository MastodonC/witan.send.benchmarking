(ns witan.send.benchmarking.sen2.requests-2026
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.benchmarking.population :as pop]))

(def file
  (io/as-file
   (io/resource "./education-health-and-care-plans_2026/data/requests.csv")))

(def table
  (-> file
      (tc/dataset {:dataset-name "sen2-2026-requests" :key-fn keyword
                   :parser-fn {:requests_received_in_year [:int32 :relaxed?]
                               :requests_rya [:int32 :relaxed?]
                               :requests_decided_to_assess [:int32 :relaxed?]
                               :requests_decided_not_to_assess [:int32 :relaxed?]
                               :requests_decision_not_made [:int32 :relaxed?]
                               :requests_withdrawn [:int32 :relaxed?]
                               :request_assess_pc [:float32 :relaxed?]
                               :request_not_assess_pc [:float32 :relaxed?]
                               :request_outstanding_pc [:float32 :relaxed?]
                               :request_withdrawn_pc [:float32 :relaxed?]
                               :request_outcome_six_weeks [:int32 :relaxed?]
                               :request_outcome_over_6_weeks [:int32 :relaxed?]
                               :request_outcome_six_weeks_pc [:float32 :relaxed?]
                               :request_outcome_over_6_weeks_pc [:float32 :relaxed?]
                               :mediation_related_request [:int32 :relaxed?]
                               :tribunal_related_request [:int32 :relaxed?]
                               :tribunal_after_mediation_request [:int32 :relaxed?]}})
      delay))

(comment

  (tc/info @table)

  (-> @table
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))

  (-> @table
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))
  ;; => sen2-2026-requests [49 2]:
  ;;    
  ;;    |                         :breakdown_topic |                               :breakdown |
  ;;    |------------------------------------------|------------------------------------------|
  ;;    | All requests for an EHC needs assessment | All requests for an EHC needs assessment |
  ;;    |               Child or young persons age |                              Age unknown |
  ;;    |               Child or young persons age |                                  Aged 10 |
  ;;    |               Child or young persons age |                                  Aged 11 |
  ;;    |               Child or young persons age |                                  Aged 12 |
  ;;    |               Child or young persons age |                                  Aged 13 |
  ;;    |               Child or young persons age |                                  Aged 14 |
  ;;    |               Child or young persons age |                                  Aged 15 |
  ;;    |               Child or young persons age |                                  Aged 16 |
  ;;    |               Child or young persons age |                                  Aged 17 |
  ;;    |               Child or young persons age |                                  Aged 18 |
  ;;    |               Child or young persons age |                                  Aged 19 |
  ;;    |               Child or young persons age |                         Aged 2 and under |
  ;;    |               Child or young persons age |                         Aged 20 and over |
  ;;    |               Child or young persons age |                                   Aged 3 |
  ;;    |               Child or young persons age |                                   Aged 4 |
  ;;    |               Child or young persons age |                                   Aged 5 |
  ;;    |               Child or young persons age |                                   Aged 6 |
  ;;    |               Child or young persons age |                                   Aged 7 |
  ;;    |               Child or young persons age |                                   Aged 8 |
  ;;    |               Child or young persons age |                                   Aged 9 |
  ;;    |                                Ethnicity |                   Any other ethnic group |
  ;;    |                                Ethnicity |       Asian - Any other Asian background |
  ;;    |                                Ethnicity |                      Asian - Bangladeshi |
  ;;    |                                Ethnicity |                          Asian - Chinese |
  ;;    |                                Ethnicity |                           Asian - Indian |
  ;;    |                                Ethnicity |                        Asian - Pakistani |
  ;;    |                                Ethnicity |       Black - Any other Black background |
  ;;    |                                Ethnicity |                    Black - Black African |
  ;;    |                                Ethnicity |                  Black - Black Caribbean |
  ;;    |                                Ethnicity |       Mixed - Any other Mixed background |
  ;;    |                                Ethnicity |                  Mixed - White and Asian |
  ;;    |                                Ethnicity |          Mixed - White and Black African |
  ;;    |                                Ethnicity |        Mixed - White and Black Caribbean |
  ;;    |                                Ethnicity |                             Unclassified |
  ;;    |                                Ethnicity |       White - Any other White background |
  ;;    |                                Ethnicity |                       White - Gypsy/Roma |
  ;;    |                                Ethnicity |                            White - Irish |
  ;;    |                                Ethnicity |      White - Traveller of Irish heritage |
  ;;    |                                Ethnicity |                    White - White British |
  ;;    |                                      Sex |                                   Female |
  ;;    |                                      Sex |                                     Male |
  ;;    |                                      Sex |                                  Unknown |
  ;;    |                        Source of request |                 Health care professional |
  ;;    |                        Source of request |                                Not known |
  ;;    |                        Source of request |                                    Other |
  ;;    |                        Source of request |                   Parent or young person |
  ;;    |                        Source of request |        School or other education setting |
  ;;    |                        Source of request |                 Social care professional |



  )


