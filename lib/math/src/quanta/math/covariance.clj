(ns quanta.math.covariance
  (:require
   [uncomplicate.neanderthal.core :as n :refer [ge]]
   [uncomplicate.neanderthal.native :refer [dge native-double]]
   [tech.v3.dataset :as ds]
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
  (let [m    (ds/row-count dataset)
        n    (count colnames)
        data (dataset->col-major-buffer dataset colnames)]
    ;; ge accepts source data and supports explicit layout.
    (ge native-double m n data {:layout :column})))

(defn ds->covariance-matrix [ds cols]
  (-> (dataset->neanderthal ds cols)
      (column-demean!)
      (covariance-matrix)))

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
