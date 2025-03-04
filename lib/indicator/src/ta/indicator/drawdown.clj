(ns ta.indicator.drawdown)

(defn xf-trailing-high [xf]
  (let [high-val (atom 0)]
    (fn
      ;; SET-UP
      ([]
       (reset! high-val 0)
       (xf))
      ;; PROCESS
      ([result input]
       (let [prior-high @high-val
             high (if (> input prior-high)
                    (reset! high-val input)
                    prior-high)]
         (xf result high)))
      ;; TEAR-DOWN
      ([result]
       (xf result)))))

(defn cumulated-return [returns-vec]
  (->>  returns-vec
        (reductions +)))

(defn drawdowns-from-value [prices]
  (let [cum-ret-high  (into [] xf-trailing-high prices)
        drawdowns  (into [] (map - cum-ret-high prices))]
    drawdowns))

(defn drawdowns [returns]
  (let [cum-ret (cumulated-return returns)
        cum-ret-high  (into [] xf-trailing-high cum-ret)
        drawdowns  (into [] (map - cum-ret-high cum-ret))]
    drawdowns))

(defn max-drawdown [returns]
  (let [dd-vec (drawdowns returns)
        dd (apply max dd-vec)]
    ;(println "dd: " dd-vec " max dd: " dd)
    dd))

(defn xf-trailing-sum [xf]
  (let [sum (atom 0)]
    (fn
      ;; SET-UP
      ([]
       (reset! sum 0)
       (xf))
      ;; PROCESS
      ([result input]
       (let [v (swap! sum + input)]
         (xf result v)))
      ;; TEAR-DOWN
      ([result]
       (xf result)))))

(defn trailing-sum [v]
  (into [] xf-trailing-sum v))

(comment

  (def returns [1 1 3 1 -2 -1 1 3])
  (def returns [-11.61, -2.824, 2.887, -7.174, 0.1188, 28.57, -2.951, 12.46, -1.684, 21.74])

  (drawdowns returns)
  (max-drawdown returns)

  (trailing-sum [1 1 1 1 1])

 ; 
  )