(ns quanta.indicator.misc
  (:require
   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.functional :as dfn]))

(defn- rf-change
  [[n prior] current]
  (if (= prior current)
    [n prior]
    [(inc n) current]))

(defn change-count [col]
  (reduce rf-change [0 nil] col))


