(ns demo.supertrend
  (:require
   [demo.env :refer [load-ds spit-ds]]
   [tech.v3.dataset :as ds]
   [quanta.indicator.supertrend :refer [supertrend add-supertrend]]
   [quanta.indicator.misc :refer [change-count]]
   
   [tablecloth.api :as tc]))

(def eurusd (load-ds "EURUSD"))

eurusd

(-> eurusd
    ;(supertrend {:atr-n 100 :atr-m 1.7})
    (add-supertrend {:atr-n 120 :atr-m 2.3})
    (tc/select-columns [:date :close :supertrend :supertrend-upper :supertrend-lower])
    (spit-ds "EURUSD")
    ;:supertrend
    ;(change-count)
    )
