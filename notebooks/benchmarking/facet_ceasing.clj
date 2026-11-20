
^:kindly/hide-code
(ns benchmarking.facet-ceasing
  (:require
   [scicloj.clay.v2.api :as clay]
   [fastmath.core :as m]
   [scicloj.plotje.api :as pj] 
   [tablecloth.api :as tc] 
   [witan.send.benchmarking.sen2.ceased-plans-2026 :as ceased-plans]))

^:kindly/hide-code
(comment (clay/make! {:source-path *file*}))

^:kindly/hide-code
(def la-name "Hertfordshire")

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
   "#db7003" "#fba600" "#f8c1a6" "#a30000" "#ff3200" "#011a51" "#97d1d9" "#916c37"])

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
        (pj/lay-point (assoc neighbour-points-options :offset-x -70))
        (pj/lay-point (assoc subject-point-options :data subject))
        geo-color-and-shape-scale
        ;;(pj/scale :y {:include 0})
        (pj/options {:title title})
        (pj/options default-chart-options))))

^:kindly/hide-code
(def overall-ceased-ehcp-per-10k
  (ceasedplans-by-reason :total_ceased))

^:kindly/hide-code
(ceasedplans-by-setting-chart overall-ceased-ehcp-per-10k :total_ceased "Overall Rate of Ceased Plans")

^:kindly/hide-code
(defn ceased-reason [reason-col reason-label]
  (-> (ceasedplans-by-reason reason-col)
      (tc/rename-columns {reason-col :ceased-plans})
      (tc/add-column :reason reason-label)))

^:kindly/hide-code
(def ceased-reason-all-year
  (tc/concat
   (ceased-reason :max_age           "Reached max age")
   (ceased-reason :needs_met         "Needs met")
   (ceased-reason :he                "Higher education")
   (ceased-reason :employ            "Employment")
   (ceased-reason :transfer          "Transferred to other LA")
   (ceased-reason :no_engage         "No longer engaging")
   (ceased-reason :moved_outside_eng "Moved outside England")
   (ceased-reason :deceased          "Deceased")
   (ceased-reason :other             "Other")))

^:kindly/hide-code
(defn ceased-reasons-facet-chart [ds title]
  (let [base (-> ds
                 (tc/add-column
                  :hover
                  #(map
                    (fn [geo-name calendar-year population ceased ceased-per-10k]
                      [:div [:b geo-name]
                       [:br]
                       "Calendar Year " calendar-year
                       [:br]
                       "Ceased EHC Plans: " (format "%,d" ceased)
                       [:br]
                       "Population: " (format "%,d" (m/round population))
                       [:br]
                       "Ceased per 10k: " (format "%,.2f" ceased-per-10k)])
                    (:geo-name %) (:calendar-year %) (:population %) (:ceased-plans %) (% "Ceased EHCPs per 10k")))
                 (tc/rename-columns sweet-column-names))
        neighbours (-> base
                       (tc/drop-rows #(= (% "Local Authority") la-name)))
        subject    (-> base
                       (tc/select-rows #(= (% "Local Authority") la-name)))]
    (-> neighbours
        (pj/lay-boxplot "Calendar Year" "Ceased EHCPs per 10k" box-plot-options)
        ;(pj/lay-point (assoc neighbour-points-options :offset-x -70))
        (pj/lay-point (assoc subject-point-options :data subject))
        geo-color-and-shape-scale
        ;(pj/scale :y {:include 0})
        (pj/options {:title title})
        (pj/options default-chart-options)
        (pj/facet :reason))))

^:kindly/hide-code
(ceased-reasons-facet-chart ceased-reason-all-year "Ceased Plans per 10k by Reason, 2022–2025")

^:kindly/hide-code
(let [base       (-> ceased-reason-all-year
                     (tc/add-column
                      :hover
                      #(map
                        (fn [geo-name reason population ceased ehcp-per-10k]
                          [:div [:b geo-name]
                           [:br]
                           reason " plans: " (format "%,d" ceased)
                           [:br]
                           "Population: " (format "%,d" (m/round population))
                           [:br]
                           "EHCP per 10k: " (format "%,.2f" ehcp-per-10k)])
                        (:geo-name %) (:reason %) (:population %) (:ceased-plans %) (% "Ceased EHCPs per 10k")))
                     (tc/rename-columns sweet-column-names))
      neighbours (-> base
                     (tc/drop-rows #(= (% "Local Authority") la-name)))
      subject    (-> base
                     (tc/select-rows #(= (% "Local Authority") la-name)))]
  (-> subject
      ;neighbours
      ;;(pj/lay-boxplot "Calendar Year" "Ceased EHCPs per 10k" box-plot-options)
      ;(pj/lay-point neighbour-points-options)
      (pj/lay-point "Calendar Year" "Ceased EHCPs per 10k" subject-point-options)
      (pj/facet :reason {:cols 3})
      geo-color-and-shape-scale
      (pj/options {:title "Reasons for Ceasing per 10k"})
      ;;(pj/scale :y {:include 0})
      (pj/options default-chart-options)))

^:kindly/hide-code
(-> ceased-reason-all-year
    (tc/add-column
     :hover
     #(map
       (fn [geo-name reason population ceased ehcp-per-10k]
         [:div [:b geo-name]
          [:br]
          reason " plans: " (format "%,d" ceased)
          [:br]
          "Population: " (format "%,d" (m/round population))
          [:br]
          "EHCP per 10k: " (format "%,.2f" ehcp-per-10k)])
       (:geo-name %) (:reason %) (:population %) (:ceased-plans %) (% "Ceased EHCPs per 10k")))
    (tc/rename-columns sweet-column-names)
    ;; (tc/select-rows (fn [r] (#{"Reached max age"} (r "Reason"))))
    (tc/select-rows (fn [r] (#{la-name} (r "Local Authority"))))
    (pj/lay-point "Calendar Year" "Ceased EHCPs per 10k" subject-point-options)
    (pj/options {:title-font-size 26, :label-font-size 18, :label-offset 50, :tooltip true,
                 :thousands-separator ",", :point-opacity 1.0, :width 1400, :height 800
                 :theme {:bg "#fff", :grid "#ddd", :font-size 14}})
    ;; geo-color-and-shape-scale
    (pj/facet :reason {:cols 3}))

^:kindly/hide-code
(defn ceased-reasons-chart [ds title]
  (let [base       (-> ds
                       (tc/add-column
                        :hover
                        #(map
                          (fn [geo-name reason population ceased ehcp-per-10k]
                            [:div [:b geo-name]
                             [:br]
                             reason " plans: " (format "%,d" ceased)
                             [:br]
                             "Population: " (format "%,d" (m/round population))
                             [:br]
                             "EHCP per 10k: " (format "%,.2f" ehcp-per-10k)])
                          (:geo-name %) (:reason %) (:population %) (:ceased-plans %) (% "Ceased EHCPs per 10k")))
                       (tc/rename-columns sweet-column-names)
                       (tc/rename-columns {:reason "Reason"}))
        neighbours (-> base
                       (tc/drop-rows #(= (% "Local Authority") la-name)))
        subject    (-> base
                       (tc/select-rows #(= (% "Local Authority") la-name)))]
    (-> neighbours
        (pj/lay-boxplot "Calendar Year" "Ceased EHCPs per 10k" box-plot-options)
        (pj/lay-point neighbour-points-options)
        (pj/lay-point (assoc subject-point-options :data subject))
        geo-color-and-shape-scale
        (pj/options {:title title})
        ;;(pj/scale :y {:include 0})
        (pj/options default-chart-options))))