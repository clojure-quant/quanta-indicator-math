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
   [uncomplicate.neanderthal.core :as n :refer [ge gd]]
   [uncomplicate.neanderthal.native :refer [dv native-double]]
   [quanta.math.neanderthal :as nm]))

(defn tail-rows
  "View of the last `n` rows of `a` (same columns). `n` must be ≤ `mrows a`."
  [a n]
  (let [m (n/mrows a)]
    (when (> n m)
      (throw (ex-info "tail-rows: n exceeds row count" {:n n :mrows m})))
    (n/submatrix a (- m n) 0 n (n/ncols a))))

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
  (let [c1 (nm/correlation-matrix (tail-rows ret-matrix 5))
        c3 (nm/correlation-matrix (tail-rows ret-matrix 15))
        c6 (nm/correlation-matrix (tail-rows ret-matrix 30))
        c12 (nm/correlation-matrix ret-matrix)]
    (nm/weight-correlation-matrices c1 c3 c6 c12)))

(def vols-vec
  (nm/column-sample-stdevs one-month))

(def covs-mat
  (nm/covariance-from-cor-and-vols cors-mat vols-vec))

;; inspect
ret-matrix

one-month

three-months

six-months

ret-subset

(nm/matrix->row-vecs cors-mat)

vols-vec

(nm/matrix->row-vecs covs-mat)
