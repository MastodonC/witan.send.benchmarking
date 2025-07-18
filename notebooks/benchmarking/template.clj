(ns benchmarking.template
  #:nextjournal.clerk{:visibility           {:code :hide, :result :hide}
                      :page-size            nil
                      :auto-expand-results? true
                      :budget               nil}
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [nextjournal.clerk :as clerk]
   [nextjournal.clerk-slideshow :as slideshow]
   [tablecloth.api :as tc]
   [witan.send.benchmarking.ceased-plans-2025 :as ceasedplans]
   [witan.send.benchmarking.newplans-2025 :as newplans]
   [witan.send.benchmarking.regional-neighbours :as rn]
   [witan.send.benchmarking.statistical-neighbours :as sn]))

(def la-name "Kent")

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

(defn plotly-ceased-neighbour-comparison
  [la-name age neighbours title max-y ceased-plans-by-age]
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
;;; # Statistical Nearest Neighbours
(clerk/row
 {::clerk/width :full}
 (clerk/table
  (-> statistical-neighbours
      (tc/select-columns [:sn :sn_name :sn_prox])
      (tc/rename-columns {:sn_name "Neighbour Name"
                          :sn "Neighbour Rank"
                          :sn_prox "Statistical Proximity"}))))

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
;;; ## Ceased Plans in Early Years

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 2 and under" statistical-neighbours-pred "Age 2 and Under w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 3" statistical-neighbours-pred "Age 3 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 4" statistical-neighbours-pred "Age 4 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))


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
;;; ## Ceased Plans in Primary

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 5" statistical-neighbours-pred "Age 5 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 6" statistical-neighbours-pred "Age 6 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 7" statistical-neighbours-pred "Age 7 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 8" statistical-neighbours-pred "Age 8 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 9" statistical-neighbours-pred "Age 9 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 10" statistical-neighbours-pred "Age 10 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))



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
;;; ## Ceased Plans in Secondary

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 11" statistical-neighbours-pred "Age 11 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 12" statistical-neighbours-pred "Age 12 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 13" statistical-neighbours-pred "Age 13 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 14" statistical-neighbours-pred "Age 14 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 15" statistical-neighbours-pred "Age 15 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 16" statistical-neighbours-pred "Age 16 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))


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
;;; ## Ceased Plans in Post 16 Ages

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 19" statistical-neighbours-pred "Age 19 w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 20 and over" statistical-neighbours-pred "Age 20 and over w/Statistical Neighbours" ceased-plans-statistical-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))


;; ---
;;; # Regional Neighbours

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
;;; ## Ceased Plans in Early Years

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 2 and under" regional-neighbours-pred "Age 2 and Under w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 3" regional-neighbours-pred "Age 3 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 4" regional-neighbours-pred "Age 4 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))


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
;;; ## Ceased Plans in Primary

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 5" regional-neighbours-pred "Age 5 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 6" regional-neighbours-pred "Age 6 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 7" regional-neighbours-pred "Age 7 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 8" regional-neighbours-pred "Age 8 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 9" regional-neighbours-pred "Age 9 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 10" regional-neighbours-pred "Age 10 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))


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
;;; ## Ceased Plans in Secondary

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 11" regional-neighbours-pred "Age 11 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 12" regional-neighbours-pred "Age 12 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 13" regional-neighbours-pred "Age 13 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 14" regional-neighbours-pred "Age 14 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 15" regional-neighbours-pred "Age 15 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 16" regional-neighbours-pred "Age 16 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))



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
;;; ## Ceased Plans in Post 16 Ages

(clerk/row
 {::clerk/width :full}
 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 19" regional-neighbours-pred "Age 19 w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la))

 (clerk/plotly
  (plotly-ceased-neighbour-comparison
   la-name "age 20 and over" regional-neighbours-pred "Age 20 and over w/Regional Neighbours" ceased-plans-regional-neighbours-max-y @ceasedplans/ceased-plans-by-age-by-la)))
