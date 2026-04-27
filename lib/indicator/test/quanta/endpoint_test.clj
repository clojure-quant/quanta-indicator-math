(ns quanta.endpoint-test
  (:require
   [clojure.test :refer :all]
   [tablecloth.api :as tc]
   [tick.core :as t]
   [quanta.indicator.endpoints :refer [endpoints]]))

(def ds
  (tc/dataset
   {:date [(t/instant #time/instant "2026-04-29T17:00:00Z")
           (t/instant #time/instant "2026-04-30T17:00:00Z")
           (t/instant #time/instant "2026-05-01T17:00:00Z")
           (t/instant #time/instant "2026-05-02T17:00:00Z")
           (t/instant #time/instant "2026-05-03T17:00:00Z")]
    :close [100 105 110 115 120]}))

(deftest test-endpoints
  (is (= (endpoints ds :month) [0 2 4])))