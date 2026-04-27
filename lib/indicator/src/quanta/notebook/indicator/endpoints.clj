(ns quanta.notebook.indicator.endpoints
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [quanta.indicator.endpoints :refer [endpoints]]))

(def ds
  (tc/dataset
   {:date [(t/instant #time/instant "2026-04-29T17:00:00Z")
           (t/instant #time/instant "2026-04-30T17:00:00Z")
           (t/instant #time/instant "2026-05-01T17:00:00Z")
           (t/instant #time/instant "2026-05-02T17:00:00Z")
           (t/instant #time/instant "2026-05-03T17:00:00Z")]
    :close [100 105 110 115 120]}))

ds

(endpoints ds :year)
(endpoints ds :month)
