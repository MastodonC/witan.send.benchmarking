(ns benchmarking.template
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.dataset.reductions :as dsr]
   [witan.send.benchmarking.sen-need-new-plans-2025 :as snnp]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as build]
   [fastmath.core :as m]
   [nextjournal.clerk :as clerk]
   [nextjournal.clerk-slideshow :as slideshow]
   [tablecloth.api :as tc]
   [tech.v3.dataset.reductions :as dsr]
   [tech.v3.datatype.functional :as dfn]
   [witan.send.benchmarking.assessment-2025 :as assessments]
   [witan.send.benchmarking.caseload-2025 :as caseload]
   [witan.send.benchmarking.ceased-plans-2025 :as ceasedplans]
   [witan.send.benchmarking.newplans-2025 :as newplans]
   [witan.send.benchmarking.regional-neighbours :as rn]
   [witan.send.benchmarking.requests-2025 :as requests]
   [witan.send.benchmarking.sen-need-new-plans-2025 :as snnp]
   [witan.send.benchmarking.statistical-neighbours :as sn]
   [witan.send.population.england :as pop])
  (:import
   (java.time LocalDateTime)
   (java.time.format DateTimeFormatter)))


(def la-name "Suffolk")

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
                          (str
                           "--"
                           (.format (LocalDateTime/now) DateTimeFormatter/ISO_DATE)
                           ".html")))
        index-out (str out-dir "index.html")]
    (clerk/build! {:paths    [in-path]
                   :ssr true
                   :bundle   true
                   :out-path out-dir})
    [(.renameTo (io/file index-out) (io/file out-path)) index-out out-path]))

^::clerk/no-cache
(def build-string 
  (format "Built from commit %s on %s"
          (build/git-process {:git-args "rev-parse --short HEAD"})
          (DateTimeFormatter/.format DateTimeFormatter/ISO_LOCAL_DATE (LocalDateTime/now))))

(comment

  (do 
    (clerk/clear-cache!)
    (when-let [path (output-ns *ns*)]
      (clerk/show! *file*)
      path))

;;; Output NS
  
  )

(clerk/add-viewers! [slideshow/viewer])

(def mc-logo-url "https://www.mastodonc.com/wp-content/themes/MastodonC-2018/dist/images/logo_mastodonc.png")

(defn mc-logo []
  (clerk/html
   {::clerk/width :full}
   [:div.fixed.bottom-0.right-12 [:img {:src mc-logo-url}]]))

(defn watermark []
  (clerk/html
   {::clerk/width :full}
   [:div.fixed.bottom-0.left-12 [:p.font-sans.italic la-name]]))

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
      ;; Join Ceased Plans
      (tc/inner-join
       (-> @ceasedplans/ceased-plans
           (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
           (tc/drop-missing [:la_name])
           (tc/select-columns [:time_period :new_la_code
                               ;; :max_age :needs_met :he :employ :transfer :no_engage :moved_outside_eng :deceased :not_rec :other
                               :transfer
                               :total_ceased])
           (tc/rename-columns {:time_period :calendar-year}))
       [:calendar-year :new_la_code])
      (tc/drop-columns #"^:sen2.*")
      ;; Join New EHCPs
      (tc/inner-join
       (-> @newplans/new-plans-by-la
           (tc/select-columns [:time_period :new_la_code :new_ehc_plans])
           (tc/rename-columns {:time_period :calendar-year}))
       [:calendar-year :new_la_code])
      (tc/drop-columns #"^:inner-join-right.*")
      (tc/map-columns :la-impact-on-plans [:ehcplans :total_ceased :new_ehc_plans]
                      (fn [ehcplans total-ceased new-ehc-plans]
                        (+ (- ehcplans total-ceased) new-ehc-plans)))
      ;; Add on Next Years Total as the plan target
      (tc/inner-join
       (-> @caseload/sen2-2025-caseload-all-ehcps
           (tc/select-columns [:calendar-year :new_la_code :total-pop :ehcplans])
           (tc/map-columns :calendar-year [:calendar-year] dec)
           (tc/rename-columns {:total-pop :year-end-total-pop
                               :ehcplans :year-end-ehcplans}))
       [:new_la_code :calendar-year])
      (tc/drop-columns #"^:inner-join-right.*")
      (tc/map-columns :transferred-in [:year-end-ehcplans :la-impact-on-plans] #(dfn/- %1 %2))
      (tc/order-by [:la_name :calendar-year])
      (tc/drop-columns [:time_period #_:total-pop :year-end-total-pop])
      #_(tc/select-columns [:calendar-year :new_la_code :la_name :ehcplans :new_ehc_plans :transferred-in :total_ceased :transfer])
      (tc/map-columns :net-transfer [:transferred-in :transfer] (fn [in out] (dfn/- in out)))
      (tc/map-columns
       :new-ehcps-per-thousand [:new_ehc_plans :total-pop]
       #(* 1000 (dfn// %1 %2)))
      (tc/map-columns
       :net-transfer-per-thousand [:net-transfer :total-pop]
       #(* 1000 (dfn// %1 %2)))
      (tc/head 500)))

(
 ;; #_(tc/map-columns :yoy-delta [:ehcplans-finish :ehcplans-start] dfn/-)
 ;; #_(tc/map-columns
 ;;    :ehcps-transferred-in
 ;;    [:yoy-delta :total_ceased :new_ehc_plans]
 ;;    (fn [delta ceased new_ehcps]
 ;;      (+ (- delta new_ehcps)
 ;;         ceased)))
 ;; #_(tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
 ;; #_(tc/map-columns
 ;;    :yoy-delta-% [:yoy-delta :ehcplans-start]
 ;;    #(dfn// %1 %2))
 ;; #_(tc/map-columns
 ;;    :ceased-%-ehcps [:total_ceased :ehcplans-start]
 ;;    #(dfn// %1 %2))
 ;; #_(tc/map-columns
 ;;    :new-plans-% [:new_ehc_plans :ehcplans-finish]
 ;;    #(dfn// %1 %2))
 ;; #_(tc/map-columns
 ;;    :ehcp-new-plans-rate [:new_ehc_plans :total-pop]
 ;;    #(dfn// %1 %2))
 ;; #_(tc/map-columns
 ;;    :ehcps-transferred-in-% [:ehcps-transferred-in :ehcplans-finish]
 ;;    #(dfn// %1 %2))
 ;; #_(tc/reorder-columns [:time_period
 ;;                        :new_la_code :la_name
 ;;                        :ehcplans-start :ehcplans-finish
 ;;                        :yoy-delta :yoy-delta-%
 ;;                        :total_ceased :ceased-%-ehcps
 ;;                        :new_ehc_plans :new-plans-% :ehcp-new-plans-rate
 ;;                        :ehcps-transferred-in :ehcps-transferred-in-%])
 ;; #_(tc/map-columns
 ;;    :cross-check [:total_ceased :new_ehc_plans :ehcps-transferred-in]
 ;;    (fn [ceased new-plans transfers]
 ;;      (- (+ new-plans transfers)
 ;;         ceased)))

 )

(def total-pop-by-la
  (-> (pop/->dataset)
      (as-> $
          (dsr/group-by-column-agg
           [:ctyua23cd :ctyua23nm :year :calendar-year]
           {:total-pop (dsr/sum :population)}
           $))
      (tc/rename-columns {:ctyua23cd :new_la_code})
      (tc/order-by [:new_la_code :calendar-year])))

(def all-ceased-plans
  (-> @caseload/sen2-2025-caseload-all-ehcps
      (tc/inner-join
       (-> @ceasedplans/ceased-plans
           (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
           (tc/drop-missing [:la_name])
           (tc/select-columns [:time_period :new_la_code :total_ceased])
           (tc/rename-columns {:time_period :calendar-year}))
       [:calendar-year :new_la_code])
      (tc/inner-join total-pop-by-la
                     [:calendar-year :new_la_code])
      (tc/map-columns
       :ceased-%-ehcps [:total_ceased :ehcplans]
       #(dfn// %1 %2))
      (tc/map-columns
       :ceased-ehcps-per-10k-pop [:total_ceased :total-pop]
       #(dfn/* 10000 (dfn// %1 %2)))))

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
       (let [age-data (-> new-plans-by-age
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
          :x-field :time_period
          :x-title "Calendar Year"
          :max-y max-y}))
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))

(defn phase-from-integer-age [age]
  (cond
    (#{0 1 2 3} age) "Early Years"
    (#{4 5 6 7 8 9 10} age) "Primary"
    (#{11 12 13 14 15 16} age) "Secondary"
    (#{17 18 19} age) "Post 16"
    (= 20 age) "Post 19"))

(def new-plans-by-phase
  (-> @newplans/new-plans-by-age-by-la
      (tc/map-columns :phase [:age-group] phase-from-integer-age)
      (as-> $
          (dsr/group-by-column-agg
           [:time_period :new_la_code :la_name :phase]
           {:new_ehc_plans (dsr/sum :new_ehc_plans)
            :population (dsr/sum :population)}
           $))
      (tc/map-columns
       :new-ehcps-per-thousand [:new_ehc_plans :population]
       #(* 1000 (dfn// %1 %2)))))

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

(def ceased-plan-per-10k
  (-> @ceasedplans/ceased-plans
      (tc/select-rows #(= "All ceased EHC plans" (:breakdown %)))
      (tc/rename-columns {:time_period :calendar-year})
      (tc/inner-join total-pop-by-la
                     [:calendar-year :new_la_code])
      (tc/map-columns :max_age_rate [:max_age :total-pop] #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :needs_met_rate [:needs_met :total-pop] #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :he_rate [:he :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :employ_rate [:employ :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :transfer_rate [:transfer :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :no_engage_rate [:no_engage :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :moved_outside_eng_rate [:moved_outside_eng :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :deceased_rate [:deceased :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :not_rec_rate [:not_rec :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/map-columns :other_rate [:other :total-pop]  #(m/approx (* 10000 (dfn// %1 %2)) 2))
      (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))))

(
;;; TODO

 ;; Needs of New EHCPs (by phase?) (and in 4/5 11/12/13)
 ;; Settings of New EHCPs (by phase?) (and in 4/5 11/12/13)

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
   [:h1.text-6xl.font-extrabold.mb-12
    (format "Benchmarking results for %s" la-name)]
   [:p.text-4xl.font-bold.italic "Presented by Mastodon C"]
   [:p build-string]
   [:p.text-3xl.mt-12 "Use ⬅️➡️ keys to navigate and ESC to see an overview."]]))

(mc-logo)

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

(watermark)
(mc-logo)

;; ---
;;; ## Total EHCP Rate vs Statistical Neighbours
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

(watermark)
(mc-logo)

;; ---
;;; ## Total EHCP Rate vs National
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @caseload/sen2-2025-caseload-all-ehcps
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

(watermark)
(mc-logo)

;; ---
;;; ## New Plans vs Neighbours
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

(watermark)
(mc-logo)

;; ---
;;; ## New Plans vs National
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @newplans/new-plans-by-la
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

(watermark)
(mc-logo)

;; ---
;;; ## New Plan Need Rates per 10,000

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @snnp/need-new-plans-rate
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/pivot->longer #"^:rate-.+" {:target-columns :need :value-column-name :rate-per-10k :drop-missing? false :coerce-to-number true})
                        (tc/map-columns :need [:need] (fn [n] (->> n name (re-find #"[^-]+$") str/upper-case)))
                        (tc/order-by [:need :la_name :year]))
    :la-name la-name
    :title "Statistical Neighbours Rate of Need per 10,000 CYP"
    :y-field :rate-per-10k
    :y-title "Rate of Need per 10,000 CYP"
    :x-field :need
    :x-title "Need"})))

(watermark)
(mc-logo)

;; ---
;;; ## New Plan Need Rates per 10,000 National

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @snnp/need-new-plans-rate
                        (tc/pivot->longer #"^:rate-.+" {:target-columns :need :value-column-name :rate-per-10k :drop-missing? false :coerce-to-number true})
                        (tc/map-columns :need [:need] (fn [n] (->> n name (re-find #"[^-]+$") str/upper-case)))
                        (tc/order-by [:need :la_name :year]))
    :la-name la-name
    :title "Statistical Neighbours Rate of Need per 10,000 CYP"
    :y-field :rate-per-10k
    :y-title "Rate of Need per 10,000 CYP"
    :x-field :need
    :x-title "Need"})))

(watermark)
(mc-logo)

;; ---
;;; ## New Plan Needs by Primary Phase
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (let [phase "Primary"]
    (neighbour-comparison-boxplot
     {:neighbour-data (-> @snnp/need-new-plans-rate-by-phase
                          (tc/select-rows #(= (:phase %) phase))
                          (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                          (tc/pivot->longer #"^:pc_.+" {:target-columns :need :value-column-name :percent-new-plans :drop-missing? false :coerce-to-number true})
                          (tc/map-columns :need [:need] (fn [n] (->> n name (re-find #"[^_]+$") str/upper-case)))
                          (tc/order-by [:need :la_name :year]))
      :la-name la-name
      :title (format "%s w/Statistical Neighbours" phase)
      :y-field :percent-new-plans
      :y-title "Need as % of New Plans"
      :x-field :need
      :x-title "Calendar Year"}))))

(watermark)
(mc-logo)

;; ---
;;; ## New Plan Needs by Secondary Phase
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (let [phase "Secondary"]
    (neighbour-comparison-boxplot
     {:neighbour-data (-> @snnp/need-new-plans-rate-by-phase
                          (tc/select-rows #(= (:phase %) phase))
                          (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                          (tc/pivot->longer #"^:pc_.+" {:target-columns :need :value-column-name :percent-new-plans :drop-missing? false :coerce-to-number true})
                          (tc/map-columns :need [:need] (fn [n] (->> n name (re-find #"[^_]+$") str/upper-case)))
                          (tc/order-by [:need :la_name :year]))
      :la-name la-name
      :title (format "%s w/Statistical Neighbours" phase)
      :y-field :percent-new-plans
      :y-title "Need as % of New Plans"
      :x-field :need
      :x-title "Calendar Year"}))))

(watermark)
(mc-logo)

;; ---
;;; ## Requests to Assess Per 1,000 CYP vs Neighbours
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

(watermark)
(mc-logo)

;; ---
;;; ## Requests to Assess Per 1,000 CYP vs National
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-request-rate
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

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

;; ---
;;; ## Percentage Requests where the LA Decided to not Assess
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:request_not_assess_pc]))
    :la-name la-name
    :title "Statistical Neighbours % of requests where LA decided to not assess"
    :y-field :request_not_assess_pc
    :y-title "% Decided to not Assess"
    :x-field :time_period
    :x-title "Calendar Year"})))

(watermark)
(mc-logo)

;; ---
;;; ## Percentage Requests where the Request Outcome Took Over 6 Weeks
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:request_outcome_over_6_weeks_pc]))
    :la-name la-name
    :title "Statistical Neighbours % of requests where the request outcome took over 6 weeks"
    :y-field :request_outcome_over_6_weeks_pc
    :y-title "% outcome over 6 weeks"
    :x-field :time_period
    :x-title "Calendar Year"})))

(watermark)
(mc-logo)

;; ---
;;; ## Percentage Assessments where a Plan was Issued
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

(watermark)
(mc-logo)

;; ---
;;; ## Percentage of EHC Plans Ceased
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> all-ceased-plans
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :ceased-%-ehcps [:ceased-%-ehcps] #(-> % (* 100) (m/approx 2))))
    :la-name la-name
    :title "Statistical Neighbours Total Ceased Plan Rate"
    :y-field :ceased-%-ehcps
    :y-title "% of EHCPs"
    :x-title "Calendar Year"})))

(watermark)
(mc-logo)

;; ---
;;; ## Rate of EHC Plans Ceased per 10,000 of Population
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> all-ceased-plans
                        (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :ceased-ehcps-per-10k-pop [:ceased-ehcps-per-10k-pop] #(-> % (m/approx 2))))
    :la-name la-name
    :title "Statistical Neighbours Total Ceased Plan Rate per 10,000 of Population"
    :y-field :ceased-ehcps-per-10k-pop
    :y-title "Ceased of EHCPs per 10,000 Population"
    :x-title "Calendar Year"})))

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

;; ---
;;; ## Ceasing Reason Comparison % of EHCPs

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

(watermark)
(mc-logo)

;; ---
;;; ## Ceasing Reason Comparison Rate per 10,000 Population

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-per-10k
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Max Age")
        :y-field :max_age_rate
        :y-title "Rate of Ceased Plans per 10,000"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-per-10k
        :la-name la-name
        :title (format "Ceasing Reason: %s" "No Engagement")
        :y-field :no_engage_rate
        :y-title "Rate of Ceased Plans per 10,000"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-per-10k
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Transferred Out")
        :y-field :transfer_rate
        :y-title "Rate of Ceased Plans per 10,000"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500))))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-per-10k
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Needs Met")
        :y-field :needs_met_rate
        :y-title "Rate of Ceased Plans per 10,000"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-per-10k
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Higher Education")
        :y-field :he_rate
        :y-title "Rate of Ceased Plans per 10,000"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500)))
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data ceased-plan-per-10k
        :la-name la-name
        :title (format "Ceasing Reason: %s" "Employed")
        :y-field :employ_rate
        :y-title "Rate of Ceased Plans per 10,000"})
      (assoc-in [:layout :height] 375)
      (assoc-in [:layout :width] 500))))

(watermark)
(mc-logo)

;; ---
;;; ## Plans Transferred In
(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> yoy-flows
      (tc/select-rows #(= la-name (:la_name %)))
      (tc/select-columns [:calendar-year :ehcplans :total_ceased :new_ehc_plans :transferred-in :net-transfer :year-end-ehcplans])
      (tc/rename-columns {:calendar-year "Calendar Year"
                          :ehcplans "Total EHC Plans"
                          :total_ceased "Total Ceased"
                          :new_ehc_plans "New EHC Plans"
                          :transferred-in "Transferred In"
                          :net-transfer "Net Transfer"
                          :year-end-ehcplans "Year End EHC Plans"}))))

(watermark)
(mc-logo)

;; ---
;;; ## Net Transferred per 1,000 CYP
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data (-> yoy-flows
                            (tc/select-rows #((conj statistical-neighbours-pred la-name) (:la_name %))))
        :la-name la-name
        :title "Net Transferred Plans Per 1,000"
        :y-field :net-transfer-per-thousand
        :y-title "Net Transferred Plans Per 1,000"}))))

(watermark)
(mc-logo)

;; ---
;;; ## New Plans by Phase
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (let [phase "Early Years"]
    (-> (neighbour-comparison-boxplot
         {:neighbour-data (tc/concat
                           (-> new-plans-by-phase
                               (tc/select-rows #(statistical-neighbours-pred (:la_name %)))
                               (tc/select-rows #(= phase (:phase %))))
                           (-> new-plans-by-phase
                               (tc/select-rows #(= la-name (:la_name %)))
                               (tc/select-rows #(= phase (:phase %)))))
          :la-name la-name
          :title (format "%s w/Statistical Neighbours" phase)
          :y-field :new-ehcps-per-thousand
          :y-title "New plans per 1,000 CYP"
          :x-field :time_period
          :x-title "Calendar Year"})
        (assoc-in [:layout :height] 375)
        (assoc-in [:layout :width] 500))))

 (clerk/plotly
  (let [phase "Primary"]
    (-> (neighbour-comparison-boxplot
         {:neighbour-data (tc/concat
                           (-> new-plans-by-phase
                               (tc/select-rows #(statistical-neighbours-pred (:la_name %)))
                               (tc/select-rows #(= phase (:phase %))))
                           (-> new-plans-by-phase
                               (tc/select-rows #(= la-name (:la_name %)))
                               (tc/select-rows #(= phase (:phase %)))))
          :la-name la-name
          :title (format "%s w/Statistical Neighbours" phase)
          :y-field :new-ehcps-per-thousand
          :y-title "New plans per 1,000 CYP"
          :x-field :time_period
          :x-title "Calendar Year"})
        (assoc-in [:layout :height] 375)
        (assoc-in [:layout :width] 500))))

 (clerk/plotly
  (let [phase "Secondary"]
    (-> (neighbour-comparison-boxplot
         {:neighbour-data (tc/concat
                           (-> new-plans-by-phase
                               (tc/select-rows #(statistical-neighbours-pred (:la_name %)))
                               (tc/select-rows #(= phase (:phase %))))
                           (-> new-plans-by-phase
                               (tc/select-rows #(= la-name (:la_name %)))
                               (tc/select-rows #(= phase (:phase %)))))
          :la-name la-name
          :title (format "%s w/Statistical Neighbours" phase)
          :y-field :new-ehcps-per-thousand
          :y-title "New plans per 1,000 CYP"
          :x-field :time_period
          :x-title "Calendar Year"})
        (assoc-in [:layout :height] 375)
        (assoc-in [:layout :width] 500)))))

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

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

(watermark)
(mc-logo)

;; ---
{::clerk/visibility {:result :hide}}
(
;;; Regional Neighbours
 )
{::clerk/visibility {:result :show}}
(clerk/html
 {::clerk/width :full}
 [:h2 (format "Neighbours for the %s region" region)])

(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> regional-neighbours
      (tc/select-columns [:la_name])
      (tc/concat (tc/dataset {:la_name [la-name]}))
      (tc/order-by [:la_name])
      (tc/rename-columns {:la_name "LA Name"}))))

;; ---
;;; ## Caseload
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @caseload/sen2-2025-caseload-all-ehcps
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :ehcp-rate [:ehcp-rate] #(-> % (* 100) (m/approx 2))))
    :la-name la-name
    :title "Regional Neighbours Total Caseload"
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
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :new-ehcps-per-thousand [:new-ehcps-per-thousand] #(m/approx % 2)))
    :la-name la-name
    :title "Regional Neighbours Total New Plan Rate"
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
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns
                         :requests-per-1000 [:requests-per-1000]
                         #(m/approx % 2)))
    :la-name la-name
    :title "Regional Neighbours % of requests per 1,000 CYP"
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
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/inner-join (-> @assessments/sen2-2025-assessments
                                           (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
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
    :title "Regional Neighbours % of requests vs assessments where a plan was issued"
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
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/inner-join (-> @newplans/newplans
                                           (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
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
    :title "Regional Neighbours % of requests vs new plans issued"
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
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:request_assess_pc]))
    :la-name la-name
    :title "Regional Neighbours % of requests where LA decided to proceed with an assessment"
    :y-field :request_assess_pc
    :y-title "% Agreed to Assess"
    :x-field :time_period
    :x-title "Calendar Year"})))


;; ---
;;; ## Percentage Requests where the LA Decided to not Assess
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:request_not_assess_pc]))
    :la-name la-name
    :title "Regional Neighbours % of requests where LA decided to not assess"
    :y-field :request_not_assess_pc
    :y-title "% Decided to not Assess"
    :x-field :time_period
    :x-title "Calendar Year"})))


;; ---
;;; ## Percentage Requests where the Request Outcome Took Over 6 Weeks
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @requests/sen2-2025-requests
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All requests for EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:request_outcome_over_6_weeks_pc]))
    :la-name la-name
    :title "Regional Neighbours % of requests where the request outcome took over 6 weeks"
    :y-field :request_outcome_over_6_weeks_pc
    :y-title "% outcome over 6 weeks"
    :x-field :time_period
    :x-title "Calendar Year"})))

;; ---
;;; ## Percentage Assessments where a Plan was Issued
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (neighbour-comparison-boxplot
   {:neighbour-data (-> @assessments/sen2-2025-assessments
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/select-rows #(= "All EHC needs assessments" (:breakdown %)))
                        (tc/drop-missing [:assess_issued_pc]))
    :la-name la-name
    :title "Regional Neighbours % of assessments where a plan was issued"
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
                        (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %)))
                        (tc/map-columns :ceased-%-ehcps [:ceased-%-ehcps] #(-> % (* 100) (m/approx 2))))
    :la-name la-name
    :title "Regional Neighbours Total Ceased Plan Rate"
    :y-field :ceased-%-ehcps
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
;;; ## Plans Transferred In
(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> yoy-flows
      (tc/select-rows #(= la-name (:la_name %)))
      (tc/select-columns [:calendar-year :ehcplans :total_ceased :new_ehc_plans :transferred-in :net-transfer :year-end-ehcplans])
      (tc/rename-columns {:calendar-year "Calendar Year"
                          :ehcplans "Total EHC Plans"
                          :total_ceased "Total Ceased"
                          :new_ehc_plans "New EHC Plans"
                          :transferred-in "Transferred In"
                          :net-transfer "Net Transfer"
                          :year-end-ehcplans "Year End EHC Plans"}))))

;; ---
;;; ## Net Transferred per 1,000 CYP
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (-> (neighbour-comparison-boxplot
       {:neighbour-data (-> yoy-flows
                            (tc/select-rows #((conj regional-neighbours-pred la-name) (:la_name %))))
        :la-name la-name
        :title "Net Transferred Plans Per 1,000"
        :y-field :net-transfer-per-thousand
        :y-title "Net Transferred Plans Per 1,000"}))))


;; ---
;;; ## New Plans by Phase
(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (let [phase "Early Years"]
    (-> (neighbour-comparison-boxplot
         {:neighbour-data (tc/concat
                           (-> new-plans-by-phase
                               (tc/select-rows #(regional-neighbours-pred (:la_name %)))
                               (tc/select-rows #(= phase (:phase %))))
                           (-> new-plans-by-phase
                               (tc/select-rows #(= la-name (:la_name %)))
                               (tc/select-rows #(= phase (:phase %)))))
          :la-name la-name
          :title (format "%s w/Regional Neighbours" phase)
          :y-field :new-ehcps-per-thousand
          :y-title "New plans per 1,000 CYP"
          :x-field :time_period
          :x-title "Calendar Year"})
        (assoc-in [:layout :height] 375)
        (assoc-in [:layout :width] 500))))

 (clerk/plotly
  (let [phase "Primary"]
    (-> (neighbour-comparison-boxplot
         {:neighbour-data (tc/concat
                           (-> new-plans-by-phase
                               (tc/select-rows #(regional-neighbours-pred (:la_name %)))
                               (tc/select-rows #(= phase (:phase %))))
                           (-> new-plans-by-phase
                               (tc/select-rows #(= la-name (:la_name %)))
                               (tc/select-rows #(= phase (:phase %)))))
          :la-name la-name
          :title (format "%s w/Regional Neighbours" phase)
          :y-field :new-ehcps-per-thousand
          :y-title "New plans per 1,000 CYP"
          :x-field :time_period
          :x-title "Calendar Year"})
        (assoc-in [:layout :height] 375)
        (assoc-in [:layout :width] 500))))

 (clerk/plotly
  (let [phase "Secondary"]
    (-> (neighbour-comparison-boxplot
         {:neighbour-data (tc/concat
                           (-> new-plans-by-phase
                               (tc/select-rows #(regional-neighbours-pred (:la_name %)))
                               (tc/select-rows #(= phase (:phase %))))
                           (-> new-plans-by-phase
                               (tc/select-rows #(= la-name (:la_name %)))
                               (tc/select-rows #(= phase (:phase %)))))
          :la-name la-name
          :title (format "%s w/Regional Neighbours" phase)
          :y-field :new-ehcps-per-thousand
          :y-title "New plans per 1,000 CYP"
          :x-field :time_period
          :x-title "Calendar Year"})
        (assoc-in [:layout :height] 375)
        (assoc-in [:layout :width] 500)))))


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

;; ---
;;; # Old Slides

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
