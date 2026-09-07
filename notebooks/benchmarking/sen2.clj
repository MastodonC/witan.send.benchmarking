^{:clay {:title "SEN2 Benchmarking" :hide-ui-header true}}
^:kindly/hide-code
(ns benchmarking.sen2
  (:require
   [fastmath.core :as m]
   [scicloj.plotje.api :as pj]
   [scicloj.kindly.v4.kind :as kind]
   [tablecloth.api :as tc]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.population :as population]
   [witan.send.benchmarking.neighbours.statistical :as sn]
   [witan.send.benchmarking.sen2.caseload-2026 :as caseload]
   [witan.send.benchmarking.sen2.newplans-2026 :as newplans]
   [witan.send.benchmarking.sen2.ceased-plans-2026 :as ceased-plans]
   [witan.send.benchmarking.sen2.timeliness-20-week :as timeliness]))

^:kindly/hide-code
(def la-name "Cornwall")

^:kindly/hide-code
(def sweet-column-names
  {:academic-year "Academic Year"
   :geo-name "Local Authority"
   :calendar-year "Calendar Year"
   :population "0-25 Population"
   :la_code "LA Code"
   :sn "Neighbour Rank"
   :sn_name "Local Authority"
   :sn_prox "Proximity"
   :ehcplans "EHC Plans"})


^:kindly/hide-code
(kind/hiccup [:h1 (format "SEN2 Benchmarking for %s" la-name)])

;;; ## LA and Neighbours

^:kindly/hide-code
(def neighbours (sn/neighbours la-name))
^:kindly/hide-code
(def neighbours-pred (sn/neighbours-name-pred la-name))

^:kindly/hide-code
(def geo-domain
  (into [la-name] (:sn_name neighbours)))

^:kindly/hide-code
(kind/hiccup [:h3 (format "Statistical Neighbours for %s" la-name)])

^:kindly/hide-code
(kind/table
 (-> neighbours
     (tc/select-columns [:sn :sn_name :sn_prox])
     (tc/rename-columns sweet-column-names)))

^:kindly/hide-code
(def colors
  ;; "Tableau 20 palette, excluding the red."
  ;; [
  ;;  ;; tableau 10
  ;;  "#1f77b4"                            ; [ 31 119 180 255]
  ;;  "#ff7f0e"                            ; [255 127  14 255]
  ;;  "#2ca02c"                            ; [ 44 160  44 255]
  ;;  #_"#d62728"                          ; [214  39  40 255]
  ;;  "#9467bd"                            ; [148 103 189 255]
  ;;  "#8c564b"                            ; [140  86  75 255]
  ;;  "#e377c2"                            ; [227 119 194 255]
  ;;  ;; "#7f7f7f"                            ; [127 127 127 255]
  ;;  "#bcbd22"                            ; [188 189  34 255]
  ;;  "#17becf"                            ; [ 23 190 207 255]
  ;;  ;; tableau 20 lighter shades
  ;;  "#aec7e8"                            ; [174 199 232 255]
  ;;  "#ffbb78"                            ; [255 187 120 255]
  ;;  "#98df8a"                            ; [152 223 138 255]
  ;;  "#ff9896"                            ; [255 152 150 255]
  ;;  "#c5b0d5"                            ; [197 176 213 255]
  ;;  "#c49c94"                            ; [196 156 148 255]
  ;;  "#f7b6d2"                            ; [247 182 210 255]
  ;;  "#c7c7c7"                            ; [199 199 199 255]
  ;;  "#dbdb8d"                            ; [219 219 141 255]
  ;;  "#9edae5"                            ; [158 218 229 255]
  ;;  ]
  ;; Partial lcars_series
  ;; (sort (map clojure2d.color/format-hex (clojure2d.color/palette :trekcolors/lcars_series)))
  #_["#000088" "#ffcc99" "#006699" "#ff9933" "#4455bb" "#cc99cc" "#664466" "#cc6666" "#9999ff" "#bbaa55" "#99ccff" "#bb4411"]

  ;; (map clojure2d.color/format-hex (clojure2d.color/palette :MoMAColors/Warhol))
  ["#ff0066" "#328c97"
   (-> "#d1aac2" (clojure2d.color/darken 0.5) clojure2d.color/format-hex)
   "#a5506d"
   (-> "#b3e0bf" (clojure2d.color/darken 0.5) clojure2d.color/format-hex)
   "#2a9d3d"
   (-> "#edf181" (clojure2d.color/darken 2.0) clojure2d.color/format-hex)
   "#db7003" "#fba600" "#f8c1a6" "#a30000" "#ff3200" "#011a51" "#97d1d9" "#916c37"]
  )

^:kindly/hide-code
(def shapes [:diamond :circle :square :triangle :cross])

^:kindly/hide-code
(def geo-domain-lookup
  (tc/dataset
   {:domain geo-domain
    :shapes (cycle shapes)
    :colors (cycle colors)}))

^:kindly/hide-code
(defn geo-color-and-shape-scale [pose]
  (-> pose
      (pj/scale :shape {:domain (:domain geo-domain-lookup)
                        :values (:shapes geo-domain-lookup)})
      (pj/scale :color {:domain (:domain geo-domain-lookup)
                        :values (:colors geo-domain-lookup)})))

^:kindly/hide-code
(def age-group-domain-lookup
  (tc/dataset
   {:domain ["Under 5" "Age 5 to 10" "Age 11 to 15" "Age 16 to 19" "Age 20 to 25"]
    :shapes (cycle shapes)
    :colors (cycle (drop 1 colors))}))

^:kindly/hide-code
(defn age-color-and-shape-scale [pose]
  (-> pose
      (pj/scale :shape {:domain (:domain age-group-domain-lookup)
                        :values (:shapes age-group-domain-lookup)})
      (pj/scale :color {:domain (:domain age-group-domain-lookup)
                        :values (:colors age-group-domain-lookup)})))

^:kindly/hide-code
(def default-chart-options
  {:title-font-size 26
   :label-font-size 18
   :label-offset 50
   ;; :legend-entry-height 50 ; this doesn't work here
   :tooltip true
   :thousands-separator ","
   :point-opacity 1.0
   :width 1400
   :theme {:bg "#fff" :grid "#ddd" :font-size 14}})

^:kindly/hide-code
(def default-home-line-options
  {:color "Local Authority"
   :x-type :categorical})

^:kindly/hide-code
(def default-neighbour-point-options
  {:color "Local Authority" :shape "Local Authority"
   :tooltip :hover
   :size 7
   :alpha 1.0
   :x-type :categorical})

^:kindly/hide-code
(def subject-point-options
  {:color "Local Authority" :shape "Local Authority"
   :tooltip :hover
   :size 9
   :alpha 1.0
   :x-type :categorical})

^:kindly/hide-code
(def neighbour-points-options
  {:color "Local Authority" :shape "Local Authority"
   :tooltip :hover
   :alpha 0.9
   :size 5
   :offset-x -40
   :x-type :categorical})

^:kindly/hide-code
(def box-plot-options
  {:x-type :categorical
   :alpha 0.2 :box-width 0.3})

;;; ## Total Population
^:kindly/hide-code
(def population-by-age
  (population/table
   :la-name-f (conj neighbours-pred la-name)
   :pipeline-f
   #(-> %
        (tc/select-rows (fn [r] (< 2018 (:calendar-year r) 2027)))
        (tc/order-by [:geo-name :age :calendar-year]))))

^:kindly/hide-code
(def population-total
  (-> (population/pop-total-by-year population-by-age :calendar-year :population)
      (tc/add-column
       :hover
       #(map
         (fn [calendar-year geo-name population]
           (format "%s %s: %,d" calendar-year geo-name (m/round population)))
         (:calendar-year %) (:geo-name %) (:population %)))
      (tc/order-by [:geo-name :calendar-year])))

^:kindly/hide-code
(-> population-total
    (tc/select-rows #(= (:geo-name %) la-name))
    (tc/rename-columns sweet-column-names)
    (pj/lay-line "Calendar Year" "0-25 Population"
                 default-home-line-options)
    (pj/lay-point "Calendar Year" "0-25 Population" subject-point-options)
    geo-color-and-shape-scale
    (pj/options default-chart-options)
    (pj/options {:title "0-25 Population"}))


^:kindly/hide-code
(let [neighbours (-> population-total
                     (tc/rename-columns sweet-column-names)
                     (tc/drop-rows #(= (% "Local Authority") la-name)))
      subject (-> population-total
                  (tc/rename-columns sweet-column-names)
                  (tc/select-rows #(= (% "Local Authority") la-name)))]
  (-> neighbours
      (pj/lay-boxplot "Calendar Year" "0-25 Population"
                      box-plot-options)
      (pj/lay-point neighbour-points-options)
      (pj/lay-point (assoc subject-point-options :data subject))
      geo-color-and-shape-scale
      (pj/scale :y {:include 0})
      (pj/options default-chart-options)
      (pj/options {:title "0-25 Population"})))

;;; ## Population by Age Group
^:kindly/hide-code
(def population-by-age-group
  (-> (population/pop-total-by-age-group population-by-age :calendar-year :population :age :age-group)
      (tc/add-column
       :hover
       #(map
         (fn [calendar-year geo-name age-group population]
           (format "%s %s %s: %,d" calendar-year geo-name age-group (m/round population)))
         (:calendar-year %) (:geo-name %) (:age-group %) (:population %)))
      (tc/order-by [:geo-name :calendar-year :age-group-order])))

^:kindly/hide-code
(-> population-by-age-group
    (tc/select-rows #(= (:geo-name %) la-name))
    (tc/rename-columns {:calendar-year "Calendar Year" :population "0-25 Population" :age-group "Age Group"})
    (pj/lay-line "Calendar Year" "0-25 Population" {:color "Age Group" :x-type :categorical})
    (pj/lay-point {:shape "Age Group" :color "Age Group" :x-type :categorical
                   :tooltip :hover
                   :alpha 1.0
                   :size 5})
    (pj/scale :shape {:domain (:domain age-group-domain-lookup)
                      :values (:shapes age-group-domain-lookup)})
    (pj/scale :color {:domain (:domain age-group-domain-lookup)
                      :values (:colors age-group-domain-lookup)})
    (pj/options default-chart-options)
    (pj/options {:title "0-25 Population by Age Group"}))

^:kindly/hide-code
(defn population-by-age-group-chart [age-group]
  (let [base (-> population-by-age-group
                 (tc/select-rows #(#{age-group} (:age-group %)))
                 (tc/rename-columns {:calendar-year "Calendar Year" :population "Population" :geo-name "Local Authority"}))
        neighbours (-> base
                       (tc/drop-rows #(= (% "Local Authority") la-name)))
        subject (-> base
                    (tc/select-rows #(= (% "Local Authority") la-name)))]
    (-> neighbours
        (pj/lay-boxplot "Calendar Year" "Population" box-plot-options)
        (pj/lay-point neighbour-points-options)
        (pj/lay-point (assoc subject-point-options :data subject))
        geo-color-and-shape-scale
        (pj/options {:title (format "%s Population" age-group)})
        (pj/options default-chart-options))))

^:kindly/hide-code
(population-by-age-group-chart "Under 5")

^:kindly/hide-code
(population-by-age-group-chart "Age 5 to 10")

^:kindly/hide-code
(population-by-age-group-chart "Age 11 to 15")

^:kindly/hide-code
(population-by-age-group-chart "Age 16 to 19")

^:kindly/hide-code
(population-by-age-group-chart "Age 20 to 25")

^:kindly/hide-code
(defn calculate [& {:keys [numerator-ds
                           denominator-ds
                           join-keys
                           input-fields
                           value-fn
                           output-field]
                    :or {value-fn #(m/approx (dfn/* 10000 (dfn// %1 %2)))}}]
  (-> numerator-ds
      (tc/inner-join denominator-ds join-keys)
      (tc/map-columns output-field input-fields value-fn)))

;;; # EHCP analysis

^:kindly/hide-code
(defn caseload-by-setting [setting]
  (calculate
   :numerator-ds
   (-> @caseload/table
       (tc/map-columns :calendar-year [:time_period] caseload/time_period->calendar-year)
       (tc/map-columns :academic-year [:time_period] caseload/time_period->academic-year)
       (tc/select-rows (fn [r] ((conj neighbours-pred la-name) (:la_name r))))
       (tc/rename-columns {:la_name :geo-name})
       (tc/select-rows (fn [r] (= "All EHC plans" (:breakdown_topic r))))
       (tc/select-columns [:time_period :calendar-year :academic-year :geo-name setting]))
   :denominator-ds
   population-total
   ;; When we have background populations by AY we should change the join
   :join-keys [:geo-name :calendar-year]
   :output-field "EHCPs per 10k"
   :input-fields [setting :population]))

^:kindly/hide-code
(defn caseload-by-setting-chart [ds ehcplan-field title]
  (let [base       (-> ds
                       (tc/add-column
                        :hover
                        #(map
                          (fn [geo-name academic-year population ehcplans ehcp-per-10k]
                            [:div [:b geo-name]
                             [:br]
                             "AY " academic-year
                             [:br]
                             "EHC Plans: " (format "%,d" ehcplans)
                             [:br]
                             "Population: " (format "%,d" (m/round population))
                             [:br]
                             "EHCP per 10k: " (format "%,.2f" ehcp-per-10k)])
                          (:geo-name %) (:academic-year %) (:population %) (ehcplan-field %) (% "EHCPs per 10k")))
                       (tc/rename-columns sweet-column-names))
        neighbours (-> base
                       (tc/drop-rows #(= (% "Local Authority") la-name)))
        subject    (-> base
                       (tc/select-rows #(= (% "Local Authority") la-name)))]
    (-> neighbours
        (pj/lay-boxplot "Academic Year" "EHCPs per 10k" {:x-type :categorical :alpha 0.2 :box-width 0.3})
        (pj/lay-point {:color    "Local Authority" :shape "Local Authority"
                       :tooltip  :hover
                       :alpha    1.0
                       :size     5
                       :offset-x -40
                       :x-type   :categorical})
        (pj/lay-point {:data    subject
                       :tooltip :hover
                       :color   "Local Authority" :shape "Local Authority"
                       :size    9
                       :alpha   1.0
                       :x-type  :categorical})
        geo-color-and-shape-scale
        (pj/options {:title title})
        (pj/scale :y {:include 0})
        (pj/options default-chart-options))))

^:kindly/hide-code
(def overall-ehcp-per-10k (caseload-by-setting :ehcplans))

^:kindly/hide-code
(caseload-by-setting-chart overall-ehcp-per-10k :ehcplans "Overall Rate of EHCPs")

^:kindly/hide-code
(def special-ehcp-per-10k
  (caseload-by-setting :special_total))

^:kindly/hide-code
(caseload-by-setting-chart special-ehcp-per-10k :special_total "Specialist Rate of EHCPs")

^:kindly/hide-code
(def mainstream-ehcp-per-10k
  (caseload-by-setting :mainstream_total))

^:kindly/hide-code
(caseload-by-setting-chart mainstream-ehcp-per-10k :mainstream_total "Mainstream Rate of EHCPs")


^:kindly/hide-code
(def ap_pru-ehcp-per-10k
  (caseload-by-setting :ap_pru_total))

^:kindly/hide-code
(caseload-by-setting-chart ap_pru-ehcp-per-10k :ap_pru_total "AP and PRU Rate of EHCPs")

^:kindly/hide-code
(def fe-ehcp-per-10k
  (caseload-by-setting :fe_total))

^:kindly/hide-code
(caseload-by-setting-chart fe-ehcp-per-10k :fe_total "Further Education Rate of EHCPs")

;;; ## New Plan Analysis

^:kindly/hide-code
(defn newplans-by-setting [setting]
  (calculate
   :numerator-ds
   (-> @newplans/table
       (tc/select-rows (fn [r] ((conj neighbours-pred la-name) (:la_name r))))
       (tc/rename-columns {:la_name :geo-name :time_period :calendar-year})
       (tc/select-rows (fn [r] (= "All new EHC plans" (:breakdown_topic r))))
       (tc/select-columns [:calendar-year :geo-name setting]))
   :denominator-ds
   population-total
   :join-keys [:geo-name :calendar-year]
   :output-field "New EHCPs per 10k"
   :input-fields [setting :population]))

^:kindly/hide-code
(defn newplans-by-setting-chart [ds ehcplan-field title]
  (let [base       (-> ds
                       (tc/add-column
                        :hover
                        #(map
                          (fn [geo-name calendar-year population ehcplans ehcp-per-10k]
                            [:div [:b geo-name]
                             [:br]
                             "Calendar Year " calendar-year
                             [:br]
                             "New EHC Plans: " (format "%,d" ehcplans)
                             [:br]
                             "Population: " (format "%,d" (m/round population))
                             [:br]
                             "New EHCPs per 10k: " (format "%,.2f" ehcp-per-10k)])
                          (:geo-name %) (:calendar-year %) (:population %) (ehcplan-field %) (% "New EHCPs per 10k")))
                       (tc/rename-columns sweet-column-names))
        neighbours (-> base
                       (tc/drop-rows #(= (% "Local Authority") la-name)))
        subject    (-> base
                       (tc/select-rows #(= (% "Local Authority") la-name)))]
    (-> neighbours
        (pj/lay-boxplot "Calendar Year" "New EHCPs per 10k" box-plot-options)
        (pj/lay-point neighbour-points-options)
        (pj/lay-point (assoc subject-point-options :data subject))
        geo-color-and-shape-scale
        (pj/scale :y {:include 0})
        (pj/options {:title title})
        (pj/options default-chart-options))))

^:kindly/hide-code
(def overall-new-ehcp-per-10k (newplans-by-setting :new_ehc_plans))

^:kindly/hide-code
(newplans-by-setting-chart overall-new-ehcp-per-10k :new_ehc_plans "Overall Rate of New EHCPs")

^:kindly/hide-code
(def special-new-ehcp-per-10k (newplans-by-setting :special_total))

^:kindly/hide-code
(newplans-by-setting-chart special-new-ehcp-per-10k :special_total "Overall Rate of New EHCPs in Specialist Settings")

^:kindly/hide-code
(def mainstream-new-ehcp-per-10k (newplans-by-setting :mainstream_total))

^:kindly/hide-code
(newplans-by-setting-chart mainstream-new-ehcp-per-10k :mainstream_total "Overall Rate of New EHCPs in Mainstream Settings")

^:kindly/hide-code
(def ap_pru-new-ehcp-per-10k (newplans-by-setting :ap_pru_total))

^:kindly/hide-code
(newplans-by-setting-chart ap_pru-new-ehcp-per-10k :ap_pru_total "Overall Rate of New EHCPs in AP & PRU Settings")

^:kindly/hide-code
(def fe-new-ehcp-per-10k (newplans-by-setting :fe_total))

^:kindly/hide-code
(newplans-by-setting-chart fe-new-ehcp-per-10k :fe_total "Overall Rate of New EHCPs in Further Education Settings")


;;; ## Ceased Plan Analysis

^:kindly/hide-code
(defn ceasedplans-by-reason [reason]
  (calculate
   :numerator-ds
   (-> @ceased-plans/table
       (tc/select-rows (fn [r] ((conj neighbours-pred la-name) (:la_name r))))
       (tc/rename-columns {:la_name :geo-name :time_period :calendar-year})
       (tc/select-rows (fn [r] (= "All ceased EHC plans" (:breakdown_topic r))))
       (tc/select-columns [:calendar-year :geo-name reason]))
   :denominator-ds
   population-total
   :join-keys [:geo-name :calendar-year]
   :output-field "Ceased EHCPs per 10k"
   :input-fields [reason :population]))

^:kindly/hide-code
(defn ceasedplans-by-setting-chart [ds ehcplan-field title]
  (let [base (-> ds
                 (tc/add-column
                  :hover
                  #(map
                    (fn [geo-name calendar-year population ehcplans ehcp-per-10k]
                      [:div [:b geo-name]
                       [:br]
                       "Calendar Year " calendar-year
                       [:br]
                       "Ceased EHC Plans: " (format "%,d" ehcplans)
                       [:br]
                       "Population: " (format "%,d" (m/round population))
                       [:br]
                       "EHCP per 10k: " (format "%,.2f" ehcp-per-10k)])
                    (:geo-name %) (:calendar-year %) (:population %) (ehcplan-field %) (% "Ceased EHCPs per 10k")))
                 (tc/rename-columns sweet-column-names))
        neighbours (-> base
                       (tc/drop-rows #(= (% "Local Authority") la-name)))
        subject    (-> base
                       (tc/select-rows #(= (% "Local Authority") la-name)))]
    (-> neighbours
        (pj/lay-boxplot "Calendar Year" "Ceased EHCPs per 10k" box-plot-options)
        (pj/lay-point neighbour-points-options)
        (pj/lay-point (assoc subject-point-options :data subject))
        geo-color-and-shape-scale
        (pj/scale :y {:include 0})
        (pj/options {:title title})
        (pj/options default-chart-options))))

^:kindly/hide-code
(def overall-ceased-ehcp-per-10k
  (ceasedplans-by-reason :total_ceased))

^:kindly/hide-code
(ceasedplans-by-setting-chart overall-ceased-ehcp-per-10k :total_ceased "Overall Rate of Ceased Plans")

;;; ## Timeliness Analysis

^:kindly/hide-code
(defn timeliness-by-category [category]
  (calculate
   :numerator-ds
   (-> @timeliness/table
       (tc/select-rows (fn [r] ((conj neighbours-pred la-name) (:la_name r))))
       (tc/rename-columns {:la_name :geo-name :time_period :calendar-year})
       (tc/select-rows (fn [r] (= "All EHC plans" (:breakdown_topic r))))
       (tc/select-columns [:calendar-year :geo-name category])
       (tc/drop-missing [category]))
   :denominator-ds
   population-total
   :join-keys [:geo-name :calendar-year]
   :output-field "EHCPs per 10k"
   :input-fields [category :population]))

^:kindly/hide-code
(defn timeliness-by-category-chart [ds title]
  (-> ds
      (tc/rename-columns sweet-column-names)
      (tc/drop-rows #(= (% "Local Authority") la-name))
      (pj/lay-boxplot "Calendar Year" "EHCPs per 10k" {:x-type :categorical :alpha 0.2 :box-width 0.3})
      (pj/lay-point {:data (-> ds
                               (tc/rename-columns sweet-column-names)
                               (tc/drop-rows #(= (% "Local Authority") la-name)))
                     :x "Calendar Year" :y "EHCPs per 10k"
                     :color "Local Authority" :shape "Local Authority"
                     ;; :jitter 4
                     :alpha 1.0
                     :size 5
                     :offset-x -40
                     :x-type :categorical})
      (pj/lay-point {:data (-> ds
                               (tc/rename-columns sweet-column-names)
                               (tc/select-rows #(= (% "Local Authority") la-name)))
                     :x "Calendar Year" :y "EHCPs per 10k"
                     :color "Local Authority" :shape "Local Authority"
                     :size 9
                     :alpha 1.0
                     :x-type :categorical})
      geo-color-and-shape-scale
      (pj/scale :y {:include 0})
      (pj/options {:title title})
      (pj/options default-chart-options)))

^:kindly/hide-code
(def plans_issued_within_20_weeks-per-10k (timeliness-by-category :plans_issued_within_20_weeks))

^:kindly/hide-code
(timeliness-by-category-chart plans_issued_within_20_weeks-per-10k "Plans Within 20wks")

^:kindly/hide-code
(def plans_issued_gt20weeks_ltyear-per-10k (timeliness-by-category :plans_issued_gt20weeks_ltyear))

^:kindly/hide-code
(timeliness-by-category-chart plans_issued_gt20weeks_ltyear-per-10k "Plans Issued Between 20wks and 1yr")


^:kindly/hide-code
(def plans_issued_gt_1_year-per-10k (timeliness-by-category :plans_issued_gt_1_year))

^:kindly/hide-code
(timeliness-by-category-chart plans_issued_gt_1_year-per-10k "Plans Issued After 1yr")
