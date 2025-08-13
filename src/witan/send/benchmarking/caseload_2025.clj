(ns witan.send.benchmarking.caseload-2025
  (:require
   [tablecloth.api :as tc]
   [tech.v3.datatype.gradient :as dt-grad]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.population.england.snpp-2022 :as pop]))

(defn time_period->calendar-year [time-period]
  (when time-period (-> time-period str (subs 4) parse-long (+ 2000))))

(def sen2-2025-caseload-filename
  "./src-data/education-health-and-care-plans_2025/data/caseload.csv")
(def sen2-2025-caseload
  (-> sen2-2025-caseload-filename
      (tc/dataset {:dataset-name "sen2-2025-caseload" :key-fn keyword
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

  )

(defn add-diffs [caseload]
  (apply tc/concat
         (into []
               (comp
                (map (fn [ds]
                       (-> ds
                           (tc/order-by [:time_period])
                           (tc/add-columns
                            {:ehcp-yoy-diff
                             (fn yoy-diff [ds]
                               (into [nil]
                                     (dt-grad/diff1d (:ehcplans ds))))
                             :ehcp-yoy-diff-%
                             (fn yoy-diff-% [ds]
                               (let [diffs   (dt-grad/diff1d (:ehcplans ds))
                                     diff-%s (dfn// diffs (drop-last (:ehcplans ds)))]
                                 (into [] cat [[nil] diff-%s])))
                             :pop-yoy-diff
                             (fn yoy-diff [ds]
                               (into [nil]
                                     (dt-grad/diff1d (:total-pop ds))))
                             :pop-yoy-diff-%
                             (fn yoy-diff-% [ds]
                               (let [diffs   (dt-grad/diff1d (:total-pop ds))
                                     diff-%s (dfn// diffs (drop-last (:total-pop ds)))]
                                 (into [] cat [[nil] diff-%s])))})))))
               (-> caseload
                   (tc/group-by [:new_la_code :la_name :breakdown_topic :breakdown] {:result-type :as-seq})))))

(comment

  (add-diffs @sen2-2025-caseload-all-ehcps)

  )

(def sen2-2025-caseload-all-ehcps
  (delay
    (-> @sen2-2025-caseload
        (tc/select-rows #(#{"All EHC plans"} (% :breakdown_topic)))
        (tc/inner-join
         (-> (pop/->witan-send-population)
             (as-> $
                 (dsr/group-by-column-agg
                  [:UTLA22CD :UTLA22NM :calendar-year]
                  {:total-pop (dsr/sum :population)}
                  $))
             (tc/rename-columns {:UTLA22CD :new_la_code})
             (tc/order-by [:new_la_code :calendar-year]))
         [:new_la_code :calendar-year])
        (tc/map-columns :ehcp-rate [:ehcplans :total-pop] dfn//))))

(def sen2-2025-caseload-all-ehcps-by-age
  (delay
    (-> @sen2-2025-caseload
        (tc/select-rows #(#{"Child or young persons age"} (% :breakdown_topic))))))


(comment

  @sen2-2025-caseload

  (tc/info @sen2-2025-caseload)
  ;; => sen2-2025-caseload: descriptive-stats [60 12]:
  ;;    |                                     :col-name | :datatype | :n-valid | :n-missing |     :min |           :mean |                      :mode |     :max | :standard-deviation |       :skew |        :first |                   :last |
  ;;    |-----------------------------------------------|-----------|---------:|-----------:|---------:|----------------:|----------------------------|---------:|--------------------:|------------:|---------------|-------------------------|
  ;;    |                                  :time_period |    :int32 |    25339 |          0 | 201819.0 | 202363.50076956 |                            | 202425.0 |         83.55793461 | -3.19870458 |        201819 |                  202425 |
  ;;    |                              :time_identifier |   :string |    25339 |          0 |          |                 |              Academic year |          |                     |             | Academic year |           Academic year |
  ;;    |                             :geographic_level |   :string |    25339 |          0 |          |                 |            Local authority |          |                     |             |      National |         Local authority |
  ;;    |                                 :country_code |   :string |    25339 |          0 |          |                 |                  E92000001 |          |                     |             |     E92000001 |               E92000001 |
  ;;    |                                 :country_name |   :string |    25339 |          0 |          |                 |                    England |          |                     |             |       England |                 England |
  ;;    |                                  :region_code |   :string |    25174 |        165 |          |                 |                  E12000007 |          |                     |             |               |               E12000009 |
  ;;    |                                  :region_name |   :string |    25174 |        165 |          |                 |                     London |          |                     |             |               |              South West |
  ;;    |                                  :new_la_code |   :string |    23708 |       1631 |          |                 |                            |          |                     |             |               |               E10000027 |
  ;;    |                                  :old_la_code |    :int16 |    23708 |       1631 |    201.0 |    618.43609752 |                            |    943.0 |        278.86131988 | -0.20905459 |               |                     933 |
  ;;    |                                      :la_name |   :string |    23708 |       1631 |          |                 |                            |          |                     |             |               |                Somerset |
  ;;    |                              :breakdown_topic |   :string |    25339 |          0 |          |                 | Child or young persons age |          |                     |             | All EHC plans | Years EHC plan in place |
  ;;    |                                    :breakdown |   :string |    25339 |          0 |          |                 |              All EHC plans |          |                     |             | All EHC plans |        Less than a year |
  ;;    |                                     :ehcplans |    :int32 |    25245 |         94 |      1.0 |   1121.19465241 |                            | 638745.0 |      10815.01109231 | 37.38294524 |        353995 |                      31 |
  ;;    |                     :mainstream_la_maintained |    :int32 |    25092 |        247 |      0.0 |    177.86493703 |                            |  95542.0 |       1777.97884342 | 34.64607349 |         64450 |                       3 |
  ;;    | :mainstream_la_maintained_resourced_provision |    :int32 |    25092 |        247 |      0.0 |     12.62936394 |                            |   6730.0 |        132.02844597 | 35.16379420 |          6214 |                       0 |
  ;;    |             :mainstream_la_maintained_senunit |    :int32 |    25092 |        247 |      0.0 |      6.67611191 |                            |   3674.0 |         73.03016607 | 33.03778632 |          3486 |                       0 |
  ;;    |                           :mainstream_academy |    :int32 |    25092 |        247 |      0.0 |    230.99673203 |                            | 141630.0 |       2208.92030371 | 37.43635768 |         51194 |                      14 |
  ;;    |       :mainstream_academy_resourced_provision |    :int32 |    25092 |        247 |      0.0 |     15.15554758 |                            |   8578.0 |        148.05590051 | 35.90120646 |          4885 |                       0 |
  ;;    |                   :mainstream_academy_senunit |    :int32 |    25092 |        247 |      0.0 |     10.29626973 |                            |   5773.0 |        105.61528073 | 33.25260163 |          2984 |                       1 |
  ;;    |                       :mainstream_free_school |    :int32 |    25092 |        247 |      0.0 |     14.10282162 |                            |   9366.0 |        133.86123906 | 38.55008609 |          1716 |                       0 |
  ;;    |   :mainstream_free_school_resourced_provision |    :int32 |    25091 |        248 |      0.0 |      0.30823801 |                            |    221.0 |          3.23810074 | 32.62439141 |            32 |                       0 |
  ;;    |               :mainstream_free_school_senunit |    :int32 |    25091 |        248 |      0.0 |      0.16009725 |                            |    104.0 |          2.13587745 | 25.71090065 |            51 |                       0 |
  ;;    |                       :mainstream_independent |    :int32 |    25092 |        247 |      0.0 |     12.80200861 |                            |   7174.0 |        124.84305533 | 35.12822305 |          3618 |                       0 |
  ;;    |                             :mainstream_total |    :int32 |    25092 |        247 |      0.0 |    480.99210904 |                            | 278236.0 |       4637.45682440 | 36.23050642 |        138630 |                      18 |
  ;;    |                          :mainstream_total_pc |    :int32 |     6375 |      18964 |      0.0 |     25.49254902 |                            |    100.0 |         30.03472364 |  0.85783827 |               |                         |
  ;;    |                        :special_la_maintained |    :int32 |    25092 |        247 |      0.0 |    174.78479197 |                            |  87350.0 |       1782.35226669 | 36.93494817 |         84033 |                       0 |
  ;;    |                         :special_academy_free |    :int32 |    25092 |        247 |      0.0 |    130.23369998 |                            |  73932.0 |       1257.26374804 | 37.21204082 |         35065 |                       0 |
  ;;    |                          :special_independent |    :int32 |    25092 |        247 |      0.0 |     50.32380839 |                            |  29647.0 |        496.47574358 | 36.19401194 |         13744 |                       1 |
  ;;    |                       :special_non_maintained |    :int32 |    25092 |        247 |      0.0 |      8.50856847 |                            |   4381.0 |         87.94860727 | 35.00093976 |          3788 |                       0 |
  ;;    |                                :special_total |    :int32 |    25092 |        247 |      0.0 |    363.85086880 |                            | 193880.0 |       3582.22218092 | 36.70316238 |        136630 |                       1 |
  ;;    |                             :special_total_pc |    :int32 |     6272 |      19067 |      0.0 |     19.74107143 |                            |    100.0 |         24.95750974 |  1.35595389 |               |                         |
  ;;    |                               :ap_pru_academy |    :int32 |    25092 |        247 |      0.0 |      3.16013072 |                            |   1967.0 |         32.67323091 | 34.03482243 |           709 |                       0 |
  ;;    |                           :ap_pru_free_school |    :int32 |    25092 |        247 |      0.0 |      1.03782082 |                            |    596.0 |         10.64949696 | 34.65165217 |           157 |                       0 |
  ;;    |                         :ap_pru_la_maintained |    :int32 |    25092 |        247 |      0.0 |      4.48493544 |                            |   2298.0 |         46.63930610 | 34.06212534 |          1865 |                       0 |
  ;;    |                                 :ap_pru_total |    :int32 |    25092 |        247 |      0.0 |      8.68288698 |                            |   4858.0 |         87.59755853 | 35.05035395 |          2731 |                       0 |
  ;;    |                              :AP_PRU_total_pc |    :int32 |    15613 |       9726 |      0.0 |      0.16454237 |                            |    100.0 |          1.95874519 | 39.23674022 |               |                       0 |
  ;;    |                 :general_fe_tertiary_colleges |    :int32 |    25092 |        247 |      0.0 |    135.90475052 |                            |  70998.0 |       1376.62771899 | 36.16767462 |         52235 |                       1 |
  ;;    |              :specialist_post_16_institutions |    :int32 |    25092 |        247 |      0.0 |     17.25916627 |                            |   9675.0 |        171.53976165 | 35.09155033 |          4956 |                       0 |
  ;;    |                               :ukrlp_provider |    :int32 |    24446 |        893 |      0.0 |     12.11012027 |                            |   8188.0 |        122.10807199 | 41.94675693 |               |                       0 |
  ;;    |                                     :fe_total |    :int32 |    25092 |        247 |      0.0 |    164.96225889 |                            |  88158.0 |       1640.00343466 | 35.88236677 |         57191 |                       1 |
  ;;    |                                  :fe_total_pc |    :int32 |     9169 |      16170 |      0.0 |     11.49296543 |                            |    100.0 |         24.07781206 |  2.36642540 |               |                         |
  ;;    |                      :elective_home_education |    :int32 |    24930 |        409 |      0.0 |     11.13112716 |                            |   7155.0 |        107.86406723 | 38.37553438 |               |                       0 |
  ;;    |                        :other_arrangements_la |    :int32 |    25092 |        247 |      0.0 |     17.38641798 |                            |  11524.0 |        168.37576418 | 38.87094435 |          2969 |                       0 |
  ;;    |                   :other_arrangements_parents |    :int32 |    25092 |        247 |      0.0 |      2.07974653 |                            |   2809.0 |         25.88161537 | 63.02913992 |          2809 |                       0 |
  ;;    |                              :online_provider |    :int32 |    12182 |      13157 |      0.0 |      0.18174356 |                            |    123.0 |          1.97699643 | 35.08917155 |               |                       0 |
  ;;    |                                   :w_settings |    :int32 |    12182 |      13157 |      0.0 |      0.59399113 |                            |    402.0 |          6.78651604 | 36.93861474 |               |                       0 |
  ;;    |                                :other_schools |    :int32 |    12182 |      13157 |      0.0 |      0.25857823 |                            |    175.0 |          2.83363925 | 34.89823526 |               |                       0 |
  ;;    |                     :other_placement_settings |    :int32 |    25188 |        151 |      0.0 |      7.34409242 |                            |   4498.0 |         75.89662158 | 35.82958887 |          1902 |                       0 |
  ;;    |                                         :neet |    :int32 |    25092 |        247 |      0.0 |     29.88570062 |                            |  18056.0 |        292.70460737 | 36.60900881 |          5876 |                       1 |
  ;;    |                                    :neet_ntci |    :int32 |    24446 |        893 |      0.0 |      2.94183097 |                            |   2268.0 |         31.79703690 | 42.00914577 |               |                       0 |
  ;;    |                                   :neet_other |    :int32 |    24446 |        893 |      0.0 |      4.51374458 |                            |   2992.0 |         46.56794045 | 38.25580385 |               |                       3 |
  ;;    |                               :neet_other_csa |    :int32 |    24446 |        893 |      0.0 |      3.14227276 |                            |   2460.0 |         32.31383180 | 41.32236061 |               |                       1 |
  ;;    |                                 :ed_elsewhere |    :int32 |    25092 |        247 |      0.0 |     76.73947872 |                            |  49750.0 |        727.63489446 | 39.69300850 |         11654 |                       5 |
  ;;    |                              :ed_elsewhere_pc |    :int32 |     6440 |      18899 |      0.0 |      9.33881988 |                            |    100.0 |         19.56209007 |  3.05369152 |               |                         |
  ;;    |                               :nm_early_years |    :int32 |    25092 |        247 |      0.0 |      7.78790053 |                            |   4524.0 |         85.80329423 | 31.95246015 |          1708 |                       6 |
  ;;    |                            :nm_early_years_pc |    :int32 |    19007 |       6332 |      0.0 |      0.92807913 |                            |    100.0 |          7.94974178 | 10.33920863 |               |                         |
  ;;    |                            :placement_unknown |    :int32 |    24446 |        893 |      0.0 |     18.29199051 |                            |  19339.0 |        242.81366720 | 48.35463476 |               |                       0 |
  ;;    |                         :placement_unknown_pc |    :int32 |    14826 |      10513 |      0.0 |      0.53898557 |                            |    100.0 |          4.73505791 | 15.66356051 |               |                       0 |
  ;;    |                              :await_prov_2022 |    :int32 |      646 |      24693 |      0.0 |     95.02012384 |                            |   6342.0 |        436.03661168 | 10.91219069 |          3486 |                         |
  ;;    |                                 :perm_ex_2022 |    :int32 |      646 |      24693 |      0.0 |      1.56037152 |                            |    121.0 |          7.79774819 |  9.76980140 |            63 |                         |

  (-> @sen2-2025-caseload
      (tc/select-columns [:breakdown_topic :breakdown])
      (tc/unique-by [:breakdown_topic :breakdown])
      (tc/order-by [:breakdown_topic :breakdown])
      (tc/head 100))
  ;; => sen2-2025-caseload [80 2]:
  ;;    |           :breakdown_topic |                          :breakdown |
  ;;    |----------------------------|-------------------------------------|
  ;;    |      Age when plan started |                              age 10 |
  ;;    |      Age when plan started |                              age 11 |
  ;;    |      Age when plan started |                              age 12 |
  ;;    |      Age when plan started |                              age 13 |
  ;;    |      Age when plan started |                              age 14 |
  ;;    |      Age when plan started |                              age 15 |
  ;;    |      Age when plan started |                              age 16 |
  ;;    |      Age when plan started |                              age 17 |
  ;;    |      Age when plan started |                              age 18 |
  ;;    |      Age when plan started |                              age 19 |
  ;;    |      Age when plan started |                     age 2 and under |
  ;;    |      Age when plan started |                     age 20 and over |
  ;;    |      Age when plan started |                               age 3 |
  ;;    |      Age when plan started |                               age 4 |
  ;;    |      Age when plan started |                               age 5 |
  ;;    |      Age when plan started |                               age 6 |
  ;;    |      Age when plan started |                               age 7 |
  ;;    |      Age when plan started |                               age 8 |
  ;;    |      Age when plan started |                               age 9 |
  ;;    |      Age when plan started |                             unknown |
  ;;    |              All EHC plans |                       All EHC plans |
  ;;    | Child or young persons age |                              age 10 |
  ;;    | Child or young persons age |                              age 11 |
  ;;    | Child or young persons age |                              age 12 |
  ;;    | Child or young persons age |                              age 13 |
  ;;    | Child or young persons age |                              age 14 |
  ;;    | Child or young persons age |                              age 15 |
  ;;    | Child or young persons age |                              age 16 |
  ;;    | Child or young persons age |                              age 17 |
  ;;    | Child or young persons age |                              age 18 |
  ;;    | Child or young persons age |                              age 19 |
  ;;    | Child or young persons age |                              age 20 |
  ;;    | Child or young persons age |                              age 21 |
  ;;    | Child or young persons age |                              age 22 |
  ;;    | Child or young persons age |                              age 23 |
  ;;    | Child or young persons age |                              age 24 |
  ;;    | Child or young persons age |                              age 25 |
  ;;    | Child or young persons age |                               age 3 |
  ;;    | Child or young persons age |                               age 4 |
  ;;    | Child or young persons age |                               age 5 |
  ;;    | Child or young persons age |                               age 6 |
  ;;    | Child or young persons age |                               age 7 |
  ;;    | Child or young persons age |                               age 8 |
  ;;    | Child or young persons age |                               age 9 |
  ;;    | Child or young persons age |                             under 3 |
  ;;    |                  Ethnicity |              Any other ethnic group |
  ;;    |                  Ethnicity |  Asian - Any other Asian background |
  ;;    |                  Ethnicity |                 Asian - Bangladeshi |
  ;;    |                  Ethnicity |                     Asian - Chinese |
  ;;    |                  Ethnicity |                      Asian - Indian |
  ;;    |                  Ethnicity |                   Asian - Pakistani |
  ;;    |                  Ethnicity |  Black - Any other Black background |
  ;;    |                  Ethnicity |               Black - Black African |
  ;;    |                  Ethnicity |             Black - Black Caribbean |
  ;;    |                  Ethnicity |  Mixed - Any other Mixed background |
  ;;    |                  Ethnicity |             Mixed - White and Asian |
  ;;    |                  Ethnicity |     Mixed - White and Black African |
  ;;    |                  Ethnicity |   Mixed - White and Black Caribbean |
  ;;    |                  Ethnicity |                        Unclassified |
  ;;    |                  Ethnicity |  White - Any other White background |
  ;;    |                  Ethnicity |                  White - Gypsy/Roma |
  ;;    |                  Ethnicity |                       White - Irish |
  ;;    |                  Ethnicity | White - Traveller of Irish heritage |
  ;;    |                  Ethnicity |               White - White British |
  ;;    |                        Sex |                              Female |
  ;;    |                        Sex |                                Male |
  ;;    |                        Sex |                             Unknown |
  ;;    |    Years EHC plan in place |                                   1 |
  ;;    |    Years EHC plan in place |                                  10 |
  ;;    |    Years EHC plan in place |                                  11 |
  ;;    |    Years EHC plan in place |                          12 or more |
  ;;    |    Years EHC plan in place |                                   2 |
  ;;    |    Years EHC plan in place |                                   3 |
  ;;    |    Years EHC plan in place |                                   4 |
  ;;    |    Years EHC plan in place |                                   5 |
  ;;    |    Years EHC plan in place |                                   6 |
  ;;    |    Years EHC plan in place |                                   7 |
  ;;    |    Years EHC plan in place |                                   8 |
  ;;    |    Years EHC plan in place |                                   9 |
  ;;    |    Years EHC plan in place |                    Less than a year |

  (into (sorted-set) (@sen2-2025-caseload :breakdown_topic))
  #{"Age when plan started" "All EHC plans" "Child or young persons age" "Ethnicity" "Sex" "Years EHC plan in place"}

  (-> @sen2-2025-caseload
      (tc/select-rows #(#{"All EHC plans"} (% :breakdown_topic)))
      (tc/head 100))

  (into (sorted-set) (-> @sen2-2025-caseload-all-ehcps-by-age :breakdown))
  #{"age 10" "age 11" "age 12" "age 13" "age 14" "age 15" "age 16" "age 17" "age 18" "age 19" "age 20" "age 21" "age 22" "age 23" "age 24" "age 25" "age 3" "age 4" "age 5" "age 6" "age 7" "age 8" "age 9" "under 3"}

  )
