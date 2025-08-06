(ns benchmarking.template
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require
   [tech.v3.datatype.gradient :as dt-grad]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [fastmath.core :as m]
   [nextjournal.clerk :as clerk]
   [nextjournal.clerk-slideshow :as slideshow]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.ceased-plans-2025 :as ceasedplans]
   [witan.send.benchmarking.newplans-2025 :as newplans]
   [witan.send.benchmarking.regional-neighbours :as rn]
   [witan.send.benchmarking.statistical-neighbours :as sn]
   [witan.send.benchmarking.caseload-2025 :as caseload]))

(def la-name "Thurrock")

(def out-dir "doc/")

(comment

  (let [ns-str (str *ns*)
        pathified-namepace (str/replace ns-str #"\.|-" {"." "/" "-" "_"})
        in-path (str "notebooks/" pathified-namepace ".clj")
        out-path (str out-dir
                      (-> la-name
                          (str/replace #"\.|-| |,"
                                       {"." "/"
                                        "-" "_"
                                        "," "_"
                                        " " "_"})
                          (str ".html")))
        index-out (str out-dir "index.html")]
    (clerk/build! {:paths    [in-path]
                   :ssr true
                   :bundle   true
                   :out-path out-dir})
    [(.renameTo (io/file index-out) (io/file out-path)) index-out out-path])

  )

(clerk/add-viewers! [slideshow/viewer])

(def region (rn/region-name la-name))
(def regional-neighbours (rn/neighbours la-name))
(def regional-neighbours-pred (rn/neighbours-name-pred la-name))

(def new-plans-regional-neighbours-max-y
  (let [neighbours regional-neighbours-pred]
    (-> (tc/concat
         (-> @newplans/new-plans-by-age-by-la
             (tc/select-rows #(#{la-name} (:la_name %))))
         (-> @newplans/new-plans-by-age-by-la
             (tc/select-rows #((set neighbours) (:la_name %)))))
        :new-ehcps-per-thousand
        (as-> $ (reduce max $))
        (as-> $ (+ $ (* $ 0.1))))))

(def ceased-plans-regional-neighbours-max-y
  (let [neighbours regional-neighbours-pred]
    (-> (tc/concat
         (-> @ceasedplans/ceased-plans-by-age-by-la
             (tc/select-rows #(#{la-name} (:la_name %))))
         (-> @ceasedplans/ceased-plans-by-age-by-la
             (tc/select-rows #((set neighbours) (:la_name %)))))
        :ceased-ehcps-per-1000-ehcps
        (as-> $ (reduce max $))
        (as-> $ (+ $ (* $ 0.1))))))


(def statistical-neighbours (sn/neighbours la-name))
(def statistical-neighbours-pred (sn/neighbours-name-pred la-name))

(def new-plans-statistical-neighbours-max-y
  (let [neighbours statistical-neighbours-pred]
    (-> (tc/concat
         (-> @newplans/new-plans-by-age-by-la
             (tc/select-rows #(#{la-name} (:la_name %))))
         (-> @newplans/new-plans-by-age-by-la
             (tc/select-rows #((set neighbours) (:la_name %)))))
        :new-ehcps-per-thousand
        (as-> $ (reduce max $))
        (as-> $ (+ $ (* $ 0.1))))))


(def yoy-flows
  (-> @caseload/sen2-2025-caseload-all-ehcps
      (tc/select-columns [:time_period :calendar-year :new_la_code :la_name :total-pop :ehcplans])
      (tc/rename-columns {:ehcplans :ehcplans-start})
      (tc/inner-join
       (-> @caseload/sen2-2025-caseload-all-ehcps
           (tc/select-columns [:time_period :calendar-year :new_la_code :la_name :ehcplans])
           (tc/update-columns {:calendar-year (partial map dec)})
           (tc/rename-columns {:ehcplans :ehcplans-finish}))
       [:calendar-year :new_la_code])
      (tc/drop-columns #"^:inner.*")
      (tc/inner-join
       (-> @ceasedplans/ceased-plans
           (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
           (tc/drop-missing [:la_name])
           (tc/select-columns [:time_period :new_la_code :total_ceased])
           (tc/rename-columns {:time_period :calendar-year}))
       [:calendar-year :new_la_code])
      (tc/inner-join
       (-> @newplans/new-plans-by-la
           (tc/select-columns [:time_period :new_la_code :new_ehc_plans])
           (tc/rename-columns {:time_period :calendar-year}))
       [:calendar-year :new_la_code])
      (tc/drop-columns #"^:sen2.*")
      (tc/map-columns :yoy-delta [:ehcplans-finish :ehcplans-start] dfn/-)
      (tc/map-columns
       :ehcps-transferred-in
       [:yoy-delta :total_ceased :new_ehc_plans]
       (fn [delta ceased new_ehcps]
         (+ (- delta new_ehcps)
            ceased)))
      (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
      (tc/map-columns
       :yoy-delta-% [:yoy-delta :ehcplans-start]
       #(dfn// %1 %2))
      (tc/map-columns
       :ceased-% [:total_ceased :ehcplans-start]
       #(dfn// %1 %2))
      (tc/map-columns
       :new-plans-% [:new_ehc_plans :ehcplans-finish]
       #(dfn// %1 %2))
      (tc/map-columns
       :ehcp-new-plans-rate [:new_ehc_plans :total-pop]
       #(dfn// %1 %2))
      (tc/map-columns
       :ehcps-transferred-in-% [:ehcps-transferred-in :ehcplans-finish]
       #(dfn// %1 %2))
      (tc/reorder-columns [:time_period :calendar-year
                           :new_la_code :la_name
                           :ehcplans-start :ehcplans-finish
                           :yoy-delta :yoy-delta-%
                           :total_ceased :ceased-%
                           :new_ehc_plans :new-plans-% :ehcp-new-plans-rate
                           :ehcps-transferred-in :ehcps-transferred-in-%])
      (tc/map-columns
       :cross-check [:total_ceased :new_ehc_plans :ehcps-transferred-in]
       (fn [ceased new-plans transfers]
         (- (+ new-plans transfers)
            ceased)))
      (tc/order-by [:la_name :calendar-year])))


(def all-ceased-plans
  (-> @caseload/sen2-2025-caseload-all-ehcps
      (tc/inner-join
       (-> @ceasedplans/ceased-plans
           (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
           (tc/drop-missing [:la_name])
           (tc/select-columns [:time_period :new_la_code :total_ceased])
           (tc/rename-columns {:time_period :calendar-year}))
       [:calendar-year :new_la_code])
      (tc/map-columns
       :ceased-% [:total_ceased :ehcplans]
       #(dfn// %1 %2))))

(comment

  (tc/head yoy-flows 500)

  )

(def ceased-plans-statistical-neighbours-max-y
  (let [neighbours statistical-neighbours-pred]
    (-> (tc/concat
         (-> @ceasedplans/ceased-plans-by-age-by-la
             (tc/select-rows #(#{la-name} (:la_name %))))
         (-> @ceasedplans/ceased-plans-by-age-by-la
             (tc/select-rows #((set neighbours) (:la_name %)))))
        :ceased-ehcps-per-1000-ehcps
        (as-> $ (reduce max $))
        (as-> $ (+ $ (* $ 0.1))))))

(defn plotly-newplan-neighbour-comparison
  [la-name age neighbours title max-y new-plans-by-age]
  (let [la-plans (-> new-plans-by-age
                     (tc/select-rows #(#{la-name} (:la_name %)))
                     (tc/select-rows #(= age (:breakdown %))))
        neighbour-plans (-> new-plans-by-age
                            (tc/select-rows #((set neighbours) (:la_name %)))
                            (tc/select-rows #(= age (:breakdown %))))]
    {:data (conj
            (transduce
             (map (fn [m] (assoc m :time_period (parse-long (:time_period m)))))
             (fn
               ([] {})
               ([acc] (into []
                            (map (fn [[k v]]
                                   {:x k
                                    :y v
                                    :name k
                                    :marker {:color "orange"}
                                    :type "box"}))
                            acc))
               ([acc x]
                (update-in acc [(:time_period x)] conj (:new-ehcps-per-thousand x))))
             (tc/rows neighbour-plans :as-maps))
            {:x (into []
                      (comp
                       (map parse-long)
                       (map (fn [y] (- (- y (rand 0.2)) 0.1))))
                      (neighbour-plans :time_period))
             :y (into [] (neighbour-plans :new-ehcps-per-thousand))
             :text (into [] (neighbour-plans  :la_name))
             :name "Neighbours"
             :marker {:color "magenta" :size 6 :symbol "square"}
             :mode "markers"
             :type "scatter"}
            {:x (into [] (map parse-long) (la-plans :time_period))
             :y (into [] (la-plans :new-ehcps-per-thousand))
             :text (into [] (la-plans :la_name))
             :name la-name
             :marker {:color "blue" :size 14 :symbol "star-diamond"}
             :mode "markers"
             :type "scatter"})
     :layout {:title {:text title}
              :scattermode "group"
              :scattergap 0.7
              :xaxis {:dtick 1 :title "SEN2 Census Year"}
              :yaxis {:rangemode "tozero" :title "New EHCPs per 1000 CYP" :range [0 max-y]}
              :height 400
              :width 500
              :showlegend false}
     :config {:displayModeBar false
              :displayLogo false}}))

(defn neighbour-comparison-boxplot
  [{:keys [neighbour-data la-name title y-field y-title]}]
  (let [la-plans (-> neighbour-data
                     (tc/select-rows #(#{la-name} (:la_name %))))
        box-data
        (transduce
         identity
         (fn
           ([] {})
           ([acc]
            (into []
                  (map (fn [[k v]]
                         {:x k
                          :y (:y v)
                          :text (:text v)
                          :name k
                          :marker {:color "orange"}
                          :boxpoints "all"
                          :pointpos -1.8
                          :jitter 0.3
                          :type "box"}))
                  acc))
           ([acc x]
            (-> acc
                (update-in [(:calendar-year x) :y] conj (y-field x))
                (update-in [(:calendar-year x) :text] conj (:la_name x)))))
         (-> neighbour-data
             (tc/drop-rows #(#{la-name} (:la_name %)))
             (tc/rows :as-maps)))]
    {:data (conj
            box-data
            {:x (into [] (la-plans :calendar-year))
             :y (into [] (la-plans y-field))
             :text (into [] (la-plans :la_name))
             :name la-name
             :marker {:color "blue" :size 14 :symbol "star-diamond"}
             :mode "markers"
             :type "scatter"})
     :layout {:title {:text title}
              :scattermode "group"
              :scattergap 0.7
              :xaxis {:dtick 1 :title "SEN2 Census Year"}
              :yaxis {:rangemode "tozero" :title y-title}
              :height 600
              :width 1400
              :showlegend false}
     :config {:displayModeBar false
              :displayLogo false}}))

(defn plotly-ceased-neighbour-comparison
  [la-name age neighbours title max-y ceased-plans-by-age]
  ;; FIXME: max-y values seem a bit broken, but I'm not sure they should be
  (let [la-plans (-> ceased-plans-by-age
                     (tc/select-rows #(#{la-name} (:la_name %)))
                     (tc/select-rows #(= age (:breakdown %))))
        neighbour-plans (-> ceased-plans-by-age
                            (tc/select-rows #((set neighbours) (:la_name %)))
                            (tc/select-rows #(= age (:breakdown %))))]
    {:data (conj
            (transduce
             (map identity)
             (fn
               ([] {})
               ([acc] (into []
                            (map (fn [[k v]]
                                   {:x k
                                    :y v
                                    :name k
                                    :marker {:color "orange"}
                                    :type "box"}))
                            acc))
               ([acc x]
                (update-in acc [(:time_period x)] conj (:ceased-ehcps-per-1000-ehcps x))))
             (tc/rows neighbour-plans :as-maps))
            {:x (into []
                      (comp
                       (map (fn [y] (- (- y (rand 0.2)) 0.1))))
                      (neighbour-plans :time_period))
             :y (into [] (neighbour-plans :ceased-ehcps-per-1000-ehcps))
             :text (into [] (neighbour-plans  :la_name))
             :name "Neighbours"
             :marker {:color "magenta" :size 6 :symbol "square"}
             :mode "markers"
             :type "scatter"}
            {:x (into [] (la-plans :time_period))
             :y (into [] (la-plans :ceased-ehcps-per-1000-ehcps))
             :text (into [] (la-plans :la_name))
             :marker {:color "blue" :size 14 :symbol "star-diamond"}
             :name la-name
             :mode "markers"
             :type "scatter"})
     :layout {:title {:text title}
              :scattermode "group"
              :scattergap 0.7
              :xaxis {:dtick 1 :title "SEN2 Census Year"}
              :yaxis {:rangemode "tozero" :title "Ceased EHCPs per 1000 EHCPs"}
              :height 400
              :width 500
              :showlegend false}
     :config {:displayModeBar false
              :displayLogo false}}))

(def ceased-plan-pc
  (-> @ceasedplans/ceased-plans
      (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
      (tc/rename-columns {:time_period :calendar-year})
      (tc/map-columns :max_age_pc [:max_age :total_ceased] #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :needs_met_pc [:needs_met :total_ceased] #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :he_pc [:he :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :employ_pc [:employ :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :transfer_pc [:transfer :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :no_engage_pc [:no_engage :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :moved_outside_eng_pc [:moved_outside_eng :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :deceased_pc [:deceased :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :not_rec_pc [:not_rec :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/map-columns :other_pc [:other :total_ceased]  #(m/approx (* 100 (dfn// %1 %2)) 2))
      (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))))

(
;;; TODO

 ;; EHCPs/Assessment
 ;; EHCPs/Requests to Assess
 ;; Asessments/Requests to Assess
 ;;
 ;; Needs of New EHCPs (by phase?) (and in 4/5 11/12/13)
 ;; Settings of New EHCPs (by phase?) (and in 4/5 11/12/13)
 ;; Raw count of new plans


 )

(
;;; Deck
 )
{::clerk/visibility {:result :show}}

(clerk/row
 {::clerk/width :full}
 (clerk/html
;;; Title Page
  {::clerk/width :full}
  [:div.max-w-screen-2xl.font-sans
   #_[:h1.text-6xl.font-extrabold (format "Benchmarking results for %s" la-name)]
   [:h1.text-6xl.font-extrabold.mb-12
    (format "Benchmarking results for %s" la-name)]
   [:p.text-4xl.font-bold.italic "Presented by Mastodon C"]
   [:p.text-3xl "Use ⬅️➡️ keys to navigate and ESC to see an overview."]]))

;; ---
;;; ## Statistical Nearest Neighbours
(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> statistical-neighbours
      (tc/select-columns [:sn :sn_name :sn_prox])
      (tc/rename-columns {:sn_name "Neighbour Name"
                          :sn "Neighbour Rank"
                          :sn_prox "Statistical Proximity"}))))

;; ---
;;; ## Caseload
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @caseload/sen2-2025-caseload-all-ehcps
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :ehcp-rate [:ehcp-rate] #(-> % (* 100) (m/approx 2))))
    :la-name la-name
    :title "Statistical Neighbours Total Caseload"
    :y-field :ehcp-rate
    :y-title "% of EHCPs"})))

;; ---
;;; ## New Plans
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @newplans/new-plans-by-la
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :new-ehcps-per-thousand [:new-ehcps-per-thousand] #(m/approx % 2)))
    :la-name la-name
    :title "Statistical Neighbours Total New Plan Rate"
    :y-field :new-ehcps-per-thousand
    :y-title "EHCPs per 1,000"})))

;; ---
;;; ## Ceased Plans
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> all-ceased-plans
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :ceased-% [:ceased-%] #(-> % (* 100) (m/approx 2))))
    :la-name la-name
    :title "Statistical Neighbours Total Ceased Plan Rate"
    :y-field :ceased-%
    :y-title "% of EHCPs"})))

;; ---
;;; ## Ceased Plan Reason Breakdown
(clerk/row
 {::clerk/width :full}
 (clerk/table
  {::clerk/width :full}
  (-> @ceasedplans/ceased-plans
      (tc/select-rows #(= la-name (:la_name %)))
      (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
      (tc/select-columns [:time_period :total_ceased :max_age :needs_met :he :employ :transfer :no_engage :moved_outside_eng :deceased :not_rec :other])
      (tc/rename-columns {:time_period "Calendar Year"
                          :total_ceased "Total Ceased"
                          :max_age "Max Age"
                          :needs_met "Needs Met"
                          :he "Higher Ed"
                          :employ "Employed"
                          :transfer "Transfer Out"
                          :no_engage "No Engagement"
                          :moved_outside_eng "Left England"
                          :deceased "Deceased"
                          :not_rec "Not Recorded"
                          :other "Other"}))))

;; ---
;;; ## Ceasing Reason Comparison

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-pc
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Max Age")
        :y-field :max_age_pc
        :y-title "% of Ceased Plans"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-pc
        :la-name la-name
        :title (format "Ceasing Reason: %s" "No Engagement")
        :y-field :no_engage_pc
        :y-title "% of Ceased Plans"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-pc
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Transferred Out")
        :y-field :transfer_pc
        :y-title "% of Ceased Plans"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500))))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-pc
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Needs Met")
        :y-field :needs_met_pc
        :y-title "% of Ceased Plans"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-pc
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Higher Education")
        :y-field :he_pc
        :y-title "% of Ceased Plans"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-pc
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Employed")
        :y-field :employ_pc
        :y-title "% of Ceased Plans"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500))))


;; ---
;;; ## New Plans in Early Years

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 2 and under" statistical-neighbours-pred "Age 2 and Under w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))

 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 3" statistical-neighbours-pred "Age 3 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))

 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 4" statistical-neighbours-pred "Age 4 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## New Plans in Primary Ages

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 5" statistical-neighbours-pred "Age 5 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 6" statistical-neighbours-pred "Age 6 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 7" statistical-neighbours-pred "Age 7 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 8" statistical-neighbours-pred "Age 8 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 9" statistical-neighbours-pred "Age 9 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 10" statistical-neighbours-pred "Age 10 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## New Plans in Secondary Ages
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 11" statistical-neighbours-pred "Age 11 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 12" statistical-neighbours-pred "Age 12 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 13" statistical-neighbours-pred "Age 13 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 14" statistical-neighbours-pred "Age 14 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 15" statistical-neighbours-pred "Age 15 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 16" statistical-neighbours-pred "Age 16 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## New Plans in Post 16 Ages
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 17" statistical-neighbours-pred "Age 17 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 18" statistical-neighbours-pred "Age 18 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 19" statistical-neighbours-pred "Age 19 w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 20 and over" statistical-neighbours-pred "Age 20+ w/Statistical Neighbours" new-plans-statistical-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## Regional Neighbours

(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> regional-neighbours
      (tc/select-columns [:la_name])
      (tc/rename-columns {:la_name "LA Name"}))))

;; ---
;;; ## New Plans in Early Years

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 2 and under" regional-neighbours-pred "Age 2 and Under w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))

 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 3" regional-neighbours-pred "Age 3 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))

 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 4" regional-neighbours-pred "Age 4 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## New Plans in Primary Ages

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 5" regional-neighbours-pred "Age 5 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 6" regional-neighbours-pred "Age 6 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 7" regional-neighbours-pred "Age 7 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 8" regional-neighbours-pred "Age 8 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 9" regional-neighbours-pred "Age 9 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 10" regional-neighbours-pred "Age 10 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## New Plans in Secondary Ages
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 11" regional-neighbours-pred "Age 11 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 12" regional-neighbours-pred "Age 12 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 13" regional-neighbours-pred "Age 13 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 14" regional-neighbours-pred "Age 14 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 15" regional-neighbours-pred "Age 15 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 16" regional-neighbours-pred "Age 16 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))

;; ---
;;; ## New Plans in Post 16 Ages
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 17" regional-neighbours-pred "Age 17 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 18" regional-neighbours-pred "Age 18 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 19" regional-neighbours-pred "Age 19 w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la))
 (clerk/plotly
  (plotly-newplan-neighbour-comparison
   la-name "age 20 and over" regional-neighbours-pred "Age 20+ w/Regional Neighbours" new-plans-regional-neighbours-max-y @newplans/new-plans-by-age-by-la)))
