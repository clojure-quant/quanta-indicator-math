(ns quanta.endpoint-test
  (:require
   [clojure.test :refer :all]
   [tablecloth.api :as tc]
   [tick.core :as t]
   [quanta.indicator.endpoints :refer [endpoints]]))

(def ds
  (tc/dataset
   {:date [(t/instant #time/instant "2026-04-29T17:00:00Z")  ; 0
           (t/instant #time/instant "2026-04-30T17:00:00Z")  ; 1
           (t/instant #time/instant "2026-05-01T17:00:00Z")  ; 2 
           (t/instant #time/instant "2026-05-02T17:00:00Z")  ; 3
           (t/instant #time/instant "2026-05-03T17:00:00Z")] ; 4
    :close [100 105 110 115 120]}))

(deftest test-endpoints-month
  ;; Last 0-based index per calendar month: April -> 1, series end -> 4.
  ;; (R xts::endpoints gives [0 2 5] for the same dates — 1-based + nrow.)
  (is (= (endpoints ds :month) [0 1 4])))

; Rscript -e 'suppressPackageStartupMessages (library (xts)); d <- as.Date(c(
; "2026-04-29","2026-04-30","2026-05-01","2026-05-02","2026-05-03"
; )); x <- xts(1:5, d); print(endpoints(x, "months"))'
; [1] 0 2 5


(def ds-year
  (tc/dataset
   {:date [(t/instant #time/instant "2025-12-29T17:00:00Z")  ; 0
           (t/instant #time/instant "2025-12-30T17:00:00Z")  ; 1
           (t/instant #time/instant "2026-01-01T17:00:00Z")  ; 2 
           (t/instant #time/instant "2026-01-02T17:00:00Z")  ; 3
           (t/instant #time/instant "2026-01-03T17:00:00Z")] ; 4
    :close [100 105 110 115 120]}))


(deftest test-endpoints-year
  (is (= (endpoints ds-year :year) [0 1 4])))