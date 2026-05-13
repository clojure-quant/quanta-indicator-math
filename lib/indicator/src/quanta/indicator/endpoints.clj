(ns quanta.indicator.endpoints
  (:require
   [tablecloth.api :as tc]
   [tick.core :as t])
  (:import
   [java.time ZoneOffset]
   [java.time.temporal WeekFields]))

(defn endpoints
  "0-based row indices of the *last* observation in each period (same breaks as
  R xts::endpoints, but R uses 1-based last rows and ends with NROW). Vector
  starts at 0 and ends at (dec row-count); skips a duplicate when the first
  period's last row is already 0. https://rdrr.io/cran/xts/man/endpoints.html"
  [ds type]
  (let [row-count (tc/row-count ds)
        last-idx (dec row-count)]
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
            (if (= (peek result) last-idx)
              result
              (conj result last-idx))
            (let [current-period (period-value (nth dates idx))]
              (if (= current-period last-period)
                (recur (inc idx) last-period result)
                (let [ep (dec idx)
                      result' (if (= ep (peek result))
                                result
                                (conj result ep))]
                  (recur (inc idx) current-period result'))))))))))



