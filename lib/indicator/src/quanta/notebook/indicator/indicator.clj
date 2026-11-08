(ns quanta.notebook.indicator.indicator
  (:require
   [ta.indicator :as ind]
   [tablecloth.api :as tc]))

(def ds
  (tc/dataset [{:open 100 :high 120 :low 90 :close 100}
               {:open 100 :high 120 :low 90 :close 101}
               {:open 100 :high 140 :low 90 :close 102}
               {:open 100 :high 140 :low 90 :close 104}
               {:open 100 :high 140 :low 90 :close 104}
               {:open 100 :high 160 :low 90 :close 106}
               {:open 100 :high 160 :low 90 :close 107}
               {:open 100 :high 160 :low 90 :close 110}]))

(ind/prior (:close ds))

(into [] (ind/sma2 3) [4 5 6 7 8 6 5 4 3])

(ind/tr ds)

  ;(ind/atr {:n 2} ds)
  ;(ind/add-atr {:n 5} ds)

(ind/sma {:n 2} (:close ds))

(ind/carry-forward [nil Double/NaN 1.0 nil nil -1.0 2.0 nil])

(ind/carry-forward-for 1 [1.0 nil nil -1.0 2.0 nil])
(ind/carry-forward-for 1 [1.0 Double/NaN nil -1.0 2.0 nil])
