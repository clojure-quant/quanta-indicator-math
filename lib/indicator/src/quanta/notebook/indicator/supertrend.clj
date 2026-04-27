(ns quanta.notebook.indicator.supertrend
  (:require
   [tablecloth.api :as tc]
   [quanta.indicator.supertrend :refer [supertrend add-supertrend]]
   [quanta.indicator.misc :refer [change-count]]
   [quanta.notebook.indicator.demodata :refer [load-ds spit-ds]]))

(def eurusd (load-ds "EURUSD"))

eurusd

(-> eurusd
    ;(supertrend {:atr-n 100 :atr-m 1.7})
    (add-supertrend {:atr-n 120 :atr-m 2.3})
    (tc/select-columns [:date :close :supertrend :supertrend-upper :supertrend-lower :supertrend-count])
    ;:supertrend
    ;(change-count)
    )

(-> eurusd
    (add-supertrend {:atr-n 120 :atr-m 2.3})
    :supertrend
    (change-count))
