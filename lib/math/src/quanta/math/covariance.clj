(ns quanta.math.covariance
  (:require
   [uncomplicate.neanderthal.core :as n :refer [ge]]
   [uncomplicate.neanderthal.native :refer [dge native-double]]
   [tech.v3.dataset :as tds]
   [tech.v3.dataset.column :as col]))

(defn matrix->row-vecs
  "Materialize Neanderthal matrix `a` as nested Clojure vectors (row-major).
   Useful for printing: Neanderthal's default printer rounds to ~2 decimals, so
   small covariances can look like all zeros."
  [a]
  (mapv (fn [i]
          (mapv (fn [j] (double (n/entry a i j)))
                (range (n/ncols a))))
        (range (n/mrows a))))

(defn column-demean!
  "Subtract each column mean in place. Returns the same matrix `a`."
  [a]
  (let [m (double (n/mrows a))]
    (doseq [j (range (n/ncols a))]
      (let [c (n/col a j)
            mu (/ (n/sum c) m)]
        (doseq [i (range (n/mrows a))]
          (n/entry! a i j (- (n/entry a i j) mu)))))
    a))

(defn covariance-matrix
  "Computes sample covariance matrix as (X'X)/(n-1), where X is already demeaned.
   Input must be a Neanderthal matrix (any engine). For constructors, use
   `uncomplicate.neanderthal.native` (e.g. `dge`) or another engine factory."
  [x]
  (let [n-rows (n/mrows x)]
    (when (< n-rows 2)
      (throw (ex-info "covariance-matrix requires at least two rows."
                      {:n-rows n-rows})))
    (let [xtx (n/mm (n/trans x) x)
          cov (n/copy xtx)]
      (n/scal (/ 1.0 (dec n-rows)) cov))))

(defn column-standardize!
  "Scale each column by 1 / sample standard deviation (divisor n-1).
   Input must already be column-demeaned. Mutates `a` in place."
  [a]
  (let [m (long (n/mrows a))
        n-1 (double (dec m))]
    (doseq [j (range (n/ncols a))]
      (let [sumsq (loop [i 0 acc 0.0]
                    (if (= i m)
                      acc
                      (let [v (double (n/entry a i j))]
                        (recur (unchecked-inc i) (+ acc (* v v))))))]
        (when (zero? sumsq)
          (throw (ex-info "column-standardize! requires positive sample variance in each column."
                          {:column j :sum-of-squares sumsq})))
        (let [s (Math/sqrt (/ sumsq n-1))]
          (dotimes [i m]
            (n/entry! a i j (/ (double (n/entry a i j)) s))))))
    a))

(defn correlation-matrix
  "Pearson sample correlation matrix of the columns of `x` (same as R `cor(x)` on a numeric matrix).
   Does not mutate `x`. Requires at least two rows."
  [x]
  (let [z (n/copy x)]
    (-> z column-demean! column-standardize! covariance-matrix)))


(defn dataset->col-major-buffer
  [dataset colnames]
  (let [m   (tech.v3.dataset/row-count dataset)
        n   (count colnames)
        out (double-array (* m n))]
    (dotimes [j n]
      (let [^doubles col (tech.v3.dataset.column/to-double-array
                          (get dataset (nth colnames j)))]
        (System/arraycopy col 0 out (* j m) m)))
    out))

(defn dataset->neanderthal
  "Convert selected numeric columns of a tech.ml.dataset/tablecloth dataset
   to a Neanderthal dense double matrix.
   Rows remain rows.
   Selected columns become matrix columns."
  [dataset colnames]
  (let [m    (tds/row-count dataset)
        n    (count colnames)
        data (dataset->col-major-buffer dataset colnames)]
    ;; ge accepts source data and supports explicit layout.
    (ge native-double m n data {:layout :column})))

(defn ds->covariance-matrix [ds cols]
  (-> (dataset->neanderthal ds cols)
      (column-demean!)
      (covariance-matrix)))

(defn ds->correlation-matrix [ds cols]
  (correlation-matrix (dataset->neanderthal ds cols)))

(comment
  (require '[tablecloth.api :as tc])
  (def ds
    (tc/dataset
     {:aapl [0.01 -0.02 0.015 0.005 -0.01]
      :msft [0.02  0.01 -0.01  0.00  0.015]
      :goog [-0.01 0.00 0.02  0.01 -0.005]}))
  ds

  (dataset->col-major-buffer ds [:aapl :msft :goog])
  (def x
    (dataset->neanderthal ds [:aapl :msft :goog]))
  x
  (println x)
  (->  (ds->covariance-matrix ds [:aapl :msft :goog])
       println))
