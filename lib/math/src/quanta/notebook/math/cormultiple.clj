(ns quanta.notebook.math.cormultiple
  "Weighted multi-horizon correlation and AAA-style cov from vol*cor (R snippet below).

   R reference:

       cors <- (cor(oneMonth[, idx]) * 12 + cor(threeMonths[, idx]) * 4 +
                cor(sixMonths[, idx]) * 2 + cor(retSubset[, idx])) / 19
       vols <- StdDev(oneMonth[, idx])
       covs <- t(vols) %*% vols * cors

   StdDev is taken as column-wise sample standard deviation (R apply(..., sd)),
   and t(vols) %*% vols * cors is the element-wise product (vol_i * vol_j * cor_ij).

   Cross-check vs R: scripts/cormultiple_r_verify.R (same returns under scripts/data/)."
  (:require
   [uncomplicate.neanderthal.core :as n :refer [ge]]
   [uncomplicate.neanderthal.native :refer [native-double]]
   [quanta.math.covariance :as cov]
   [quanta.math.stats :as stats]))

(defn tail-rows
  "View of the last `n` rows of `a` (same columns). `n` must be ≤ `mrows a`."
  [a n]
  (let [m (n/mrows a)]
    (when (> n m)
      (throw (ex-info "tail-rows: n exceeds row count" {:n n :mrows m})))
    (n/submatrix a (- m n) 0 n (n/ncols a))))

(defn column-sample-stdevs
  "Vector of column sample standard deviations (divisor n-1), length = `ncols a`."
  [a]
  (let [m (n/mrows a)]
    (mapv (fn [j]
            (stats/standard-deviation
             (mapv #(double (n/entry a % j)) (range m))))
          (range (n/ncols a)))))

(defn fast-correlation-average-matrix
  "Weighted average of Pearson correlation matrices on tail windows, matching the R
   `(12*C1 + 4*C3 + 2*C6 + C60) / 19` construction. Pass the full return matrix `a`
   (e.g. 60×p); windows are last 5, 15, 30, and all rows of `a`."
  [a]
  (let [c1 (cov/correlation-matrix (tail-rows a 5))
        c3 (cov/correlation-matrix (tail-rows a 15))
        c6 (cov/correlation-matrix (tail-rows a 30))
        c0 (cov/correlation-matrix a)
        p (n/ncols a)
        out (ge native-double p p (double-array (* p p)) {:layout :column})]
    (dotimes [i p]
      (dotimes [j p]
        (n/entry! out i j
                  (+ (/ (* 12.0 (n/entry c1 i j)) 19.0)
                     (/ (* 4.0 (n/entry c3 i j)) 19.0)
                     (/ (* 2.0 (n/entry c6 i j)) 19.0)
                     (/ (n/entry c0 i j) 19.0)))))
    out))

(defn covariance-from-cor-and-vols
  "Rebuild covariance from correlation `cor-mat` and per-asset volatilities `vol-vec`
   (length p): `cov_ij = vol_i * vol_j * cor_ij`, i.e. R `t(vols) %*% vols * cors` with
   element-wise final multiply."
  [cor-mat vol-vec]
  (let [p (count vol-vec)
        out (ge native-double p p (double-array (* p p)) {:layout :column})]
    (dotimes [i p]
      (dotimes [j p]
        (n/entry! out i j
                  (* (double (nth vol-vec i))
                     (double (nth vol-vec j))
                     (n/entry cor-mat i j)))))
    out))

(defn random-returns-matrix
  "Dense `m`×`p` matrix of pseudo-random Gaussian returns (column-major)."
  [^long m ^long p ^long seed]
  (let [rng (java.util.Random. seed)
        buf (double-array (* m p))]
    (dotimes [k (alength buf)]
      (aset buf k (* 0.015 (.nextGaussian rng))))
    (ge native-double m p buf {:layout :column})))

;; ---- demo: 5 assets, 60 days; windows match R names ----------------------------

(def ret-seed 42)
(def ret-count 60)
(def asset-count 5)

(def ret-matrix
  (random-returns-matrix ret-count asset-count ret-seed))

(def one-month
  (tail-rows ret-matrix 5))

(def three-months
  (tail-rows ret-matrix 15))

(def six-months
  (tail-rows ret-matrix 30))

(def ret-subset
  ret-matrix)

(def cors-mat
  (fast-correlation-average-matrix ret-matrix))

(def vols-vec
  (column-sample-stdevs one-month))

(def covs-mat
  (covariance-from-cor-and-vols cors-mat vols-vec))

;; inspect
ret-matrix

one-month

three-months

six-months

ret-subset

(cov/matrix->row-vecs cors-mat)

vols-vec

(cov/matrix->row-vecs covs-mat)
