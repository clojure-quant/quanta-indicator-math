(ns quanta.indicator.supertrend
  (:require
   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.functional :as dfn]
   [tablecloth.api :as tc]
   [ta.indicator :as ind]
   [tech.v3.dataset :as tds]))

(defn calc-trend []
  (let [trend (volatile! 1)
        monotonic-upper (volatile! nil)
        monotonic-lower (volatile! nil)]
    (fn [{:keys [close upper lower]}]
      ;(println "calculating close: " close " upper: " upper " lower: " lower)
      ; in an uptrend the lower band can only increase
      (when (and (= @trend 1)
                 (or (nil? @monotonic-lower)
                     (> lower @monotonic-lower)))
        (vreset! monotonic-lower lower))

      ; in a downtrend, the higher band can only decrease      
      (when (and (= @trend -1)
                 (or (nil? @monotonic-upper)
                     (< upper @monotonic-upper)))
        (vreset! monotonic-upper upper))

      ; downtrend + up breakout
      (when (and (= @trend -1) (> close @monotonic-upper))
        (vreset! trend 1)
        (vreset! monotonic-upper nil)
        (vreset! monotonic-lower lower))

      ; uptrend + down breakout
      (when  (and (= @trend 1) (< close @monotonic-lower))
        (vreset! trend -1)
        (vreset! monotonic-lower nil)
        (vreset! monotonic-upper upper))

      ; output columns
      {:supertrend @trend
       :supertrend-upper @monotonic-upper
       :supertrend-lower @monotonic-lower})))

(defn prior-double [price n-ago]
  (let [l (count price)]
    (dtype/make-reader
     :float64 l
     (if (>= idx n-ago)
       (price (- idx n-ago))
       (price idx)))))

(defn supertrend [bar-ds {:keys [atr-n atr-m]
                          :or {atr-m 1.0}}]
  (assert atr-n "supertrend needs :atr-n")
  (let [atr (ind/atr {:n atr-n} bar-ds)
        delta (dfn/* atr atr-m)
        close (:close bar-ds)
        upper (dfn/+ close delta)
        lower (dfn/- close delta)
        ds-level (tc/add-columns bar-ds {:upper upper :lower lower})
        ;level-up-breakout-1 (prior-double level-up-breakout 1)
        ;level-down-breakout-1 (prior-double level-down-breakout 1)
        trend-fn (calc-trend)
        v (->> (tds/mapseq-reader ds-level)
               (map trend-fn)
               (into []))
        ;(tc/map-rows ds-level trend-fn)
        ;lsupertrend (dtype/clone (:supertrend ds2))
        ;supertrend-upper (dtype/clone (:supertrend-upper ds2))
        ;supertrend-lower (dtype/clone (:supertrend-lower ds2))
        ]
;(tc/add-columns bar-ds {:supertrend supertrend
                            ;:supertrend-upper supertrend-upper
                            ;:supertrend-lower supertrend-lower
                            ;})
    (tc/dataset v)))

(defn add-supertrend [bar-ds {:keys [atr-n atr-m]
                              :or {atr-m 1.0}
                              :as opts}]
  (let [supertrend-ds (supertrend bar-ds opts)
        supertrend (:supertrend supertrend-ds)
        supertrend-upper (:supertrend-upper supertrend-ds)
        supertrend-lower (:supertrend-lower supertrend-ds)]
    (tc/add-columns bar-ds {:supertrend supertrend
                            :supertrend-upper supertrend-upper
                            :supertrend-lower supertrend-lower})))


