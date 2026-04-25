(ns quanta.notebook.math.demo-data
  (:require
   [tablecloth.api :as tc]))

(def returns-ds
  (tc/dataset
   {:date ["2026-01-01" "2026-01-02" "2026-01-03" "2026-01-04" "2026-01-05"]
    "AAPL" [0.01 0.02 0.03 0.04 0.05]
    "MSFT" [0.06 0.07 0.08 0.09 0.10]
    "GOOG" [0.11 0.12 0.13 0.14 0.15]}))

returns-ds
