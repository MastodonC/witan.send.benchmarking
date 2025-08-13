(ns benchmarking.template
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [fastmath.core :as m]
   [nextjournal.clerk :as clerk]
   [nextjournal.clerk-slideshow :as slideshow]
   [tablecloth.api :as tc]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.assessment-2025 :as assessments]
   [witan.send.benchmarking.ceased-plans-2025 :as ceasedplans]
   [witan.send.benchmarking.newplans-2025 :as newplans]
   [witan.send.benchmarking.requests-2025 :as requests]
   [witan.send.benchmarking.regional-neighbours :as rn]
   [witan.send.benchmarking.statistical-neighbours :as sn]
   [witan.send.benchmarking.caseload-2025 :as caseload]))

(def la-name "Kent")

(def out-dir "doc/")

(defn output-ns [ns]
  (let [ns-str (str ns)
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
    [(.renameTo (io/file index-out) (io/file out-path)) index-out out-path]))

(comment

  (output-ns *ns*)

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

(defn neighbour-comparison-boxplot
  [{:keys [neighbour-data la-name title y-field y-title x-field x-title max-y]
    :or {x-field :calendar-year
         x-title "SEN2 Census Year"}}]
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
                (update-in [(x-field x) :y] conj (y-field x))
                (update-in [(x-field x) :text] conj (:la_name x)))))
         (-> neighbour-data
             (tc/drop-rows #(#{la-name} (:la_name %)))
             (tc/rows :as-maps)))]
    {:data (conj
            box-data
            {:x (into [] (la-plans x-field))
             :y (into [] (la-plans y-field))
             :text (into [] (la-plans :la_name))
             :name la-name
             :marker {:color "blue" :size 14 :symbol "star-diamond"}
             :mode "markers"
             :type "scatter"})
     :layout {:title {:text title}
              :scattermode "group"
              :scattergap 0.7
              :xaxis {:dtick 1 :title x-title}
              :yaxis {:rangemode "tozero" :title y-title :range (when max-y [0 max-y])}
              :height 600
              :width 1400
              :showlegend false}
     :config {:displayModeBar false
              :displayLogo false}}))

(defn plotly-newplan-neighbour-comparison
  [la-name age neighbours title max-y new-plans-by-age]
  (-> (neighbour-comparison-boxplot
       (let [neighbours statistical-neighbours-pred
             max-y new-plans-statistical-neighbours-max-y
             age-data (-> new-plans-by-age
                          (tc/select-rows #(= age (:breakdown %))))]
         {:neighbour-data (tc/concat
                           (-> age-data
                               (tc/select-rows #(#{la-name} (:la_name %))))
                           (-> age-data
                               (tc/select-rows #((set neighbours) (:la_name %)))))
          :la-name la-name
          :title (or title (format "%s w/Statistical Neighbours" (str/capitalize age)))
          :y-field :new-ehcps-per-thousand
          :y-title "New EHCPs per 1,000 CYP"
          :max-y max-y}))
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))

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
    :y-title "% of EHCPs"}))
 (clerk/col
  (clerk/md "### Number of EHCPs")
  (clerk/table 
   (-> @caseload/sen2-2025-caseload-all-ehcps
       (tc/select-rows #(= la-name (:la_name %)))
       (tc/select-columns [:calendar-year :ehcplans])
       (tc/order-by [:calendar-year])
       (tc/rename-columns {:calendar-year "SEN2 Census Year" :ehcplans "EHC Plans"})))))

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
    :y-title "EHCPs per 1,000"
    :x-field :time_period
    :x-title "Calendar Year"}))
 (clerk/col
  (clerk/md "### New Plans Issued")
  (clerk/table
   (-> @newplans/new-plans-by-la
       (tc/select-rows #(= la-name (:la_name %)))
       (tc/select-columns [:time_period :new_ehc_plans])
       (tc/order-by [:time_period])
       (tc/rename-columns {:time_period "Year" :new_ehc_plans "Plans Issued"})))))

;; ---
;;; ## Requests to Assess Per 1,000 CYP
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-request-rate
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns
                         :requests-per-1000 [:requests-per-1000]
                         #(m/approx % 2)))
    :la-name la-name
    :title "Statistical Neighbours % of requests per 1,000 CYP"
    :y-field :requests-per-1000
    :y-title "Requests per 1,000 CYP"
    :x-field :time_period
    :x-title "Calendar Year"}))
 (clerk/col
  (clerk/md "### Requests Received")
  (clerk/table
   (-> @requests/sen2-2025-request-rate
       (tc/select-rows #(= la-name (:la_name %)))
       (tc/select-columns [:time_period :requests_received_in_year])
       (tc/order-by [:time_period])
       (tc/rename-columns {:time_period "Year" :requests_received_in_year "Requests"})))))

;; ---
;;; ## Requests vs Decision to Issue Plan
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/inner-join (-> @assessments/sen2-2025-assessments
                                           (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                                           (tc/select-rows #(= "All EHC needs assessments" (:breakdown %)))
                                           (tc/drop-missing [:assess_issued]))
                                       [:time_period :la_name])
                        (tc/drop-missing [:assess_issued :requests_received_in_year])
                        (tc/map-columns
                         :pct-request-to-plan [:assess_issued :requests_received_in_year]
                         #(if (zero? %2)
                            nil
                            (-> (dfn// %1 %2)
                                (dfn/* 100)
                                (m/approx 2)))))
    :la-name la-name
    :title "Statistical Neighbours % of requests vs assessments where a plan was issued"
    :y-field :pct-request-to-plan
    :y-title "% Request where Plan Issued"
    :x-field :time_period
    :x-title "Calendar Year"})))

;; ---
;;; ## Requests vs New EHC Plans
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/map-columns :time_period [:time_period] str)
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/inner-join (-> @newplans/newplans
                                           (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                                           (tc/select-rows #(= (:breakdown %) "New EHC plans")))
                                       [:time_period :la_name])
                        (tc/drop-missing [:new_ehc_plans :requests_received_in_year])
                        (tc/map-columns
                         :pct-request-to-plan [:new_ehc_plans :requests_received_in_year]
                         #(if (zero? %2)
                            nil
                            (-> (dfn// %1 %2)
                                (dfn/* 100)
                                (m/approx 2)))))
    :la-name la-name
    :title "Statistical Neighbours % of requests vs new plans issued"
    :y-field :pct-request-to-plan
    :y-title "% Request where Plan Issued"
    :x-field :time_period
    :x-title "Calendar Year"})))


;; ---
;;; ## Percentage Requests where LA Decided to Proceed with an Assessment
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:request_assess_pc]))
    :la-name la-name
    :title "Statistical Neighbours % of requests where LA decided to proceed with an assessment"
    :y-field :request_assess_pc
    :y-title "% Agreed to Assess"
    :x-field :time_period
    :x-title "Calendar Year"})))

;; ---
;;; ## Percentage Requests where a Plan was Issued
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @assessments/sen2-2025-assessments
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:assess_issued_pc]))
    :la-name la-name
    :title "Statistical Neighbours % of assessments where a plan was issued"
    :y-field :assess_issued_pc
    :y-title "% Plans Issued"
    :x-field :time_period
    :x-title "Calendar Year"})))

;; ---
;;; ## Percentage of EHC Plans Ceased
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
    :y-title "% of EHCPs"
    :x-title "Calendar Year"})))

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
