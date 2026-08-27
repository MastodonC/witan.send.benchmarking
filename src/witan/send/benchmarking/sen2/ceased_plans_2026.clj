(ns witan.send.benchmarking.sen2.ceased-plans-2026
  (:require
   [clojure.java.io :as io]
   [tablecloth.api :as tc]))

(def file
  (-> "./education-health-and-care-plans_2026/data/ceased_plans.csv"
      io/resource
      io/as-file))

(def table
  (delay
    (-> file
        (tc/dataset {:dataset-name "sen2-2026-ceased-plans"
                     :key-fn keyword
                     :parser-fn {:time_period :int64
                                 :time_identifier :geographic_level
                                 :country_code :string
                                 :country_name :string
                                 :region_code :string
                                 :region_name :string
                                 :new_la_code :string
                                 :old_la_code :string
                                 :la_name :string
                                 :breakdown_topic :string
                                 :breakdown :string
                                 :max_age [:int32 :relaxed?]
                                 :needs_met [:int32 :relaxed?]
                                 :he [:int32 :relaxed?]
                                 :employ [:int32 :relaxed?]
                                 :transfer [:int32 :relaxed?]
                                 :no_engage [:int32 :relaxed?]
                                 :moved_outside_eng [:int32 :relaxed?]
                                 :deceased [:int32 :relaxed?]
                                 :other [:int32 :relaxed?]
                                 :total_ceased [:int32 :relaxed?]}}))))

(comment
  (-> @table
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 500))

  (tc/column-names @table)


  ;; => sen2-2026-ceased-plans [107 2]:
  ;;    
  ;;    |     :breakdown_topic |                                                                 :breakdown |
  ;;    |----------------------|----------------------------------------------------------------------------|
  ;;    |      Age plan ceased |                                                                Age unknown |
  ;;    |      Age plan ceased |                                                                     age 10 |
  ;;    |      Age plan ceased |                                                                     age 11 |
  ;;    |      Age plan ceased |                                                                     age 12 |
  ;;    |      Age plan ceased |                                                                     age 13 |
  ;;    |      Age plan ceased |                                                                     age 14 |
  ;;    |      Age plan ceased |                                                                     age 15 |
  ;;    |      Age plan ceased |                                                                     age 16 |
  ;;    |      Age plan ceased |                                                                     age 17 |
  ;;    |      Age plan ceased |                                                                     age 18 |
  ;;    |      Age plan ceased |                                                                     age 19 |
  ;;    |      Age plan ceased |                                                            age 2 and under |
  ;;    |      Age plan ceased |                                                            age 20 and over |
  ;;    |      Age plan ceased |                                                                      age 3 |
  ;;    |      Age plan ceased |                                                                      age 4 |
  ;;    |      Age plan ceased |                                                                      age 5 |
  ;;    |      Age plan ceased |                                                                      age 6 |
  ;;    |      Age plan ceased |                                                                      age 7 |
  ;;    |      Age plan ceased |                                                                      age 8 |
  ;;    |      Age plan ceased |                                                                      age 9 |
  ;;    | All ceased EHC plans |                                                       All ceased EHC plans |
  ;;    |            Ethnicity |                                                     Any other ethnic group |
  ;;    |            Ethnicity |                                         Asian - Any other Asian background |
  ;;    |            Ethnicity |                                                        Asian - Bangladeshi |
  ;;    |            Ethnicity |                                                            Asian - Chinese |
  ;;    |            Ethnicity |                                                             Asian - Indian |
  ;;    |            Ethnicity |                                                          Asian - Pakistani |
  ;;    |            Ethnicity |                                         Black - Any other Black background |
  ;;    |            Ethnicity |                                                      Black - Black African |
  ;;    |            Ethnicity |                                                    Black - Black Caribbean |
  ;;    |            Ethnicity |                                         Mixed - Any other Mixed background |
  ;;    |            Ethnicity |                                                    Mixed - White and Asian |
  ;;    |            Ethnicity |                                            Mixed - White and Black African |
  ;;    |            Ethnicity |                                          Mixed - White and Black Caribbean |
  ;;    |            Ethnicity |                                                               Unclassified |
  ;;    |            Ethnicity |                                         White - Any other White background |
  ;;    |            Ethnicity |                                                         White - Gypsy/Roma |
  ;;    |            Ethnicity |                                                              White - Irish |
  ;;    |            Ethnicity |                                        White - Traveller of Irish heritage |
  ;;    |            Ethnicity |                                                      White - White British |
  ;;    | Primary type of need |                                                 Autistic spectrum disorder |
  ;;    | Primary type of need |                                                              Down Syndrome |
  ;;    | Primary type of need |                                                         Hearing impairment |
  ;;    | Primary type of need |                                               Moderate learning difficulty |
  ;;    | Primary type of need |                                                   Multi-sensory impairment |
  ;;    | Primary type of need |                                                               Not reported |
  ;;    | Primary type of need |                                             Other difficulty or disability |
  ;;    | Primary type of need |                                                        Physical disability |
  ;;    | Primary type of need |                                  Profound and multiple learning difficulty |
  ;;    | Primary type of need |                                                 Severe learning difficulty |
  ;;    | Primary type of need |                                        Social, emotional and mental health |
  ;;    | Primary type of need |                                               Specific learning difficulty |
  ;;    | Primary type of need |                                   Speech, language and communication needs |
  ;;    | Primary type of need |                                                          Vision impairment |
  ;;    |                  Sex |                                                                     Female |
  ;;    |                  Sex |                                                                       Male |
  ;;    |    Type of placement |                                                     AP/PRU - LA maintained |
  ;;    |    Type of placement |                                                           AP/PRU - academy |
  ;;    |    Type of placement |                                                       AP/PRU - free school |
  ;;    |    Type of placement |                                                             AP/PRU - total |
  ;;    |    Type of placement |                                                  Educated elsewhere - NEET |
  ;;    |    Type of placement |                                   Educated elsewhere - Welsh establishment |
  ;;    |    Type of placement |                               Educated elsewhere - elective home education |
  ;;    |    Type of placement | Educated elsewhere - not in education or training (notice to cease issued) |
  ;;    |    Type of placement |                  Educated elsewhere - not in education or training (other) |
  ;;    |    Type of placement |                                       Educated elsewhere - online provider |
  ;;    |    Type of placement |                              Educated elsewhere - other arrangements by LA |
  ;;    |    Type of placement |                         Educated elsewhere - other arrangements by parents |
  ;;    |    Type of placement |                              Educated elsewhere - other placement settings |
  ;;    |    Type of placement |                      Educated elsewhere - other schools and establishments |
  ;;    |    Type of placement |                                                 Educated elsewhere - total |
  ;;    |    Type of placement |                                                        FE - UKRLP provider |
  ;;    |    Type of placement |                                      FE - general FE and tertiary colleges |
  ;;    |    Type of placement |                                       FE - specialist post-16 institutions |
  ;;    |    Type of placement |                                                                 FE - total |
  ;;    |    Type of placement |                                                 Mainstream - LA maintained |
  ;;    |    Type of placement |                                      Mainstream - LA maintained (SEN unit) |
  ;;    |    Type of placement |                           Mainstream - LA maintained (resourced provision) |
  ;;    |    Type of placement |                                                       Mainstream - academy |
  ;;    |    Type of placement |                                            Mainstream - academy (SEN unit) |
  ;;    |    Type of placement |                                 Mainstream - academy (resourced provision) |
  ;;    |    Type of placement |                                                   Mainstream - free school |
  ;;    |    Type of placement |                                        Mainstream - free school (SEN unit) |
  ;;    |    Type of placement |                             Mainstream - free school (resourced provision) |
  ;;    |    Type of placement |                                                   Mainstream - independent |
  ;;    |    Type of placement |                                                         Mainstream - total |
  ;;    |    Type of placement |                                                 Non-maintained early years |
  ;;    |    Type of placement |                                                     Placement not recorded |
  ;;    |    Type of placement |                                                          Special - Academy |
  ;;    |    Type of placement |                                                      Special - Free school |
  ;;    |    Type of placement |                                                    Special - LA maintained |
  ;;    |    Type of placement |                                                      Special - independent |
  ;;    |    Type of placement |                                                   Special - non-maintained |
  ;;    |    Type of placement |                                                            Special - total |
  ;;    |      Years plan held |                                                                     1 year |
  ;;    |      Years plan held |                                                                   10 years |
  ;;    |      Years plan held |                                                                   11 years |
  ;;    |      Years plan held |                                                          12 years and over |
  ;;    |      Years plan held |                                                                    2 years |
  ;;    |      Years plan held |                                                                    3 years |
  ;;    |      Years plan held |                                                                    4 years |
  ;;    |      Years plan held |                                                                    5 years |
  ;;    |      Years plan held |                                                                    6 years |
  ;;    |      Years plan held |                                                                    7 years |
  ;;    |      Years plan held |                                                                    8 years |
  ;;    |      Years plan held |                                                                    9 years |
  ;;    |      Years plan held |                                                               Under a year |



  )
