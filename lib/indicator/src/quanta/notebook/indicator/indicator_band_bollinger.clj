(ns quanta.notebook.indicator-band-bollinger
  (:require
   [tablecloth.api :as tc]
   [ta.indicator.band :as band]))

(def ds1
  (tc/dataset {:close [100.0 101.0 103.0 102.0 104.0 105.0]}))

(band/add-bollinger {:n 2 :m 3.0} ds1)