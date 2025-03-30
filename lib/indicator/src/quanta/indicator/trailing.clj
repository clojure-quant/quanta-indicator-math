(ns quanta.indicator.trailing
  (:require
   [tech.v3.datatype :as dtype]
   [tech.v3.dataset.rolling :as r]
   [tablecloth.api :as tc]))

(defn- add-delayed-col [ds {:keys [col offset]
                            :or {offset 1}}]
  (let [col (col ds)
        col-delayed (dtype/clone
                     (dtype/make-reader :float64 (count col) (if (>= idx offset)
                                                               (col (- idx offset))
                                                               Double/NaN)))]
    (tc/add-column ds :delayed col-delayed)))

(defn- nan> [a b]
  (if (and a b (not (NaN? a)) (not (NaN? b)))
    (> a b)
    false))

(defn add-trailing-high
  "add :trailing-high (over n bars) and :trailing-high? which 
   is true if the current bar :high is higher that the :trailing-high"
  [ds n]
  (let [delayed-ds (add-delayed-col ds {:col :high})
        trailing-high-ds (r/rolling delayed-ds
                                    {:window-type :fixed
                                     :window-size n
                                     :relative-window-position :left}
                                    {:trailing-high (r/max :delayed)})
        trailing-high (:trailing-high trailing-high-ds)
        trailing-high? (dtype/clone (dtype/emap nan> :bool (:high ds) trailing-high))]
    (tc/add-columns ds {:trailing-high trailing-high
                        :trailing-high? trailing-high?})))

(defn- nan< [a b]
  (if (and a b (not (NaN? a)) (not (NaN? b)))
    (< a b)
    false))

(defn add-trailing-low
  "add :trailing-low (over n bars) and :trailing-low? which 
   is true if the current bar :low is lower that the :trailing-low"
  [ds n]
  (let [delayed-ds (add-delayed-col ds {:col :low})
        trailing-low-ds (r/rolling delayed-ds
                                   {:window-type :fixed
                                    :window-size n
                                    :relative-window-position :left}
                                   {:trailing-low (r/min :delayed)})
        trailing-low (:trailing-low trailing-low-ds)
        trailing-low? (dtype/clone (dtype/emap nan< :bool (:low ds) trailing-low))]
    (tc/add-columns ds {:trailing-low trailing-low
                        :trailing-low? trailing-low?})))

(comment
  (def ds
    (tc/dataset {:high [1.1 2.2 3.3 4.4 5.5 6.6 7.7 6.1 5.1 4.1 3.1 4.1 5.1]
                 :low [1.1 2.2 3.3 4.4 5.5 6.6 7.7 6.1 5.1 4.1 3.1 4.1 5.1]}))

  (add-delayed-col ds {:col :high})
  (add-delayed-col ds {:col :low})

  (add-trailing-high ds 3)
  (add-trailing-low ds 3)

; 
  )


