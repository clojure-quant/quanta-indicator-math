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

(defn column-sample-stdevs
  "Per-column sample standard deviation (divisor n-1), same as R `apply(a, 2, sd)`.

  After `column-demean!`, column `j` has squared Euclidean norm equal to `(n-1)` times
  its sample variance, so `stdev_j = (nrm2 (col a j)) / sqrt(n-1)` — only Neanderthal
  BLAS/vector ops for the numerics; one Clojure pass over column indices to gather values."
  [a]
  (let [m (long (n/mrows a))]
    (when (< m 2)
      (throw (ex-info "column-sample-stdevs requires at least two rows." {:n-rows m})))
    (let [xc (n/copy a)
          sqrt-n-1 (Math/sqrt (double (dec m)))]
      (nm/column-demean! xc)
      (mapv (fn [^long j]
              (/ (double (n/nrm2 (n/col xc j))) sqrt-n-1))
            (range (n/ncols xc))))))

(defn weight-correlation-matrices
  "Weighted average of neanderthal Pearson correlation matrices,
   matching the R `(12*C1 + 4*C3 + 2*C6 + C12) / 19` formula"
  [c1 c3 c6 c12]
  (let [p (long (n/ncols c12))
        out (ge native-double p p (double-array (* p p)) {:layout :column})]
    (n/axpy! 12.0 c1 out)
    (n/axpy! 4.0 c3 out)
    (n/axpy! 2.0 c6 out)
    (n/axpy! 1.0 c12 out)
    (n/scal! (/ 1.0 19.0) out)
    out))

(defn covariance-from-cor-and-vols
  "Rebuild covariance from correlation `cor-mat` and per-asset volatilities `vol-vec`
   (length p): `cov_ij = vol_i * vol_j * cor_ij`, i.e. R `t(vols) %*% vols * cors` with
   element-wise final multiply.

   Implemented as two BLAS matrix multiplies: `Cov = D^T * (Cor * D)` with diagonal
   `D = diag(vol)` from `gd`. For diagonal `D`, `trans(D)` matches `D`; the transpose
   makes the same layout as R's `t(vols) %*% ...` explicit."
  [cor-mat vol-vec]
  (let [p (long (count vol-vec))
        D (gd native-double p (dv vol-vec))
        Dt (n/trans D)
        tmp (ge native-double p p (double-array (* p p)) {:layout :column})
        out (ge native-double p p (double-array (* p p)) {:layout :column})]
    (n/mm! 1.0 cor-mat D 0.0 tmp)
    (n/mm! 1.0 Dt tmp 0.0 out)
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
  (let [c1 (nm/correlation-matrix (tail-rows ret-matrix 5))
        c3 (nm/correlation-matrix (tail-rows ret-matrix 15))
        c6 (nm/correlation-matrix (tail-rows ret-matrix 30))
        c12 (nm/correlation-matrix ret-matrix)]
    (weight-correlation-matrices c1 c3 c6 c12)))

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

(nm/matrix->row-vecs cors-mat)

vols-vec

(nm/matrix->row-vecs covs-mat)
