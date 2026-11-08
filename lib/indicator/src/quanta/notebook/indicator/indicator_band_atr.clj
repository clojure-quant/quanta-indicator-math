(ns quanta.notebook.indicator.indicator-band-atr
  (:require
   [tablecloth.api :as tc]
   [ta.indicator.band :as band]))

(def ds
  (tc/dataset [{:open 100 :high 120 :low 90 :close 100}
               {:open 100 :high 120 :low 90 :close 101}
               {:open 100 :high 140 :low 90 :close 102}
               {:open 100 :high 140 :low 90 :close 104}
               {:open 100 :high 140 :low 90 :close 104}
               {:open 100 :high 160 :low 90 :close 106}
               {:open 100 :high 160 :low 90 :close 107}
               {:open 100 :high 160 :low 90 :close 110}]))

(band/add-atr-band {:atr-n 5 :atr-m 2.0} ds)
