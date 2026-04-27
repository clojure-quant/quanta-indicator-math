(ns quanta.indicator.endpoints
  (:require
   [tablecloth.api :as tc]
   [tick.core :as t])
  (:import
   [java.time ZoneOffset]
   [java.time.temporal WeekFields]))

(defn endpoints [ds type]
  (let [row-count (tc/row-count ds)]
    (if (zero? row-count)
      []
      (let [period-value (case type
                           :year #(-> % t/year .getValue)
                           :month #(-> % t/month .getValue)
                           :week (fn [inst]
                                   (let [week-fields WeekFields/ISO
                                         zdt (.atZone inst ZoneOffset/UTC)
                                         week (.get zdt (.weekOfWeekBasedYear week-fields))
                                         week-year (.get zdt (.weekBasedYear week-fields))]
                                     [week-year week]))
                           (throw (ex-info "Unsupported endpoint type"
                                           {:type type
                                            :supported-types [:year :month :week]})))
            dates (:date ds)]
        (loop [idx 1
               last-period (period-value (nth dates 0))
               result [0]]
          (if (= idx row-count)
            (if (= (peek result) (dec row-count))
              result
              (conj result (dec row-count)))
            (let [current-period (period-value (nth dates idx))]
              (if (= current-period last-period)
                (recur (inc idx) last-period result)
                (recur (inc idx) current-period (conj result idx))))))))))



