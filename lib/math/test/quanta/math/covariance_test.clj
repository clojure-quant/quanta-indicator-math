(ns quanta.math.covariance-test
  "Covariance / correlation match R `cov` / `cor` for the demo matrix in scripts/returns_correlation_covariance.R."
  (:require
   [clojure.test :refer [deftest is testing]]
   [quanta.math.covariance :as cov]
   [uncomplicate.neanderthal.core :refer [ge]]
   [uncomplicate.neanderthal.native :refer [native-double]]))

(defn- columns->col-major-doubles ^doubles [cols]
  (let [m (count (first cols))
        n (count cols)
        out (double-array (* m n))]
    (dotimes [j n]
      (let [col (nth cols j)]
        (dotimes [i m]
          (aset out (+ (* j m) i) (double (nth col i))))))
    out))

(defn- near-matrix? [eps expected-rows actual-rows]
  (every? (fn [[e a]] (< (Math/abs (- e a)) eps))
          (map vector (flatten expected-rows) (flatten actual-rows))))

;; Same numeric layout as R: matrix(..., nrow = 10, ncol = 3) fills by column.
(def ^:private demo-returns-cols
  '[[0.01 -0.02 0.0 0.015 -0.005 0.008 -0.012 0.003 0.006 -0.004]
    [0.005 0.01 -0.008 0.002 0.012 0.004 0.007 -0.009 0.001 0.011]
    [-0.01 0.003 0.009 -0.006 0.004 -0.011 0.002 0.01 -0.003 0.005]])

;; R: cov(returns), sample divisor (n - 1). Same data as scripts/returns_correlation_covariance.R.
;; Values from R dput(cov(m)) (default print rounds; these are the full doubles R uses).
(def ^:private r-cov-rows
  [[0.000113211111111111 -3.33888888888889e-05 -4.33666666666667e-05]
   [-3.33888888888889e-05 5.36111111111111e-05 -1.49444444444444e-05]
   [-4.33666666666667e-05 -1.49444444444444e-05 5.55666666666667e-05]])

;; R: cor(returns). Values from R dput(cor(m)).
(def ^:private r-cor-rows
  [[1.0 -0.428578160402653 -0.546769578324926]
   [-0.428578160402653 1.0 -0.273807410652732]
   [-0.546769578324926 -0.273807410652732 1.0]])

(defn- demo-returns-matrix []
  (ge native-double 10 3 (columns->col-major-doubles demo-returns-cols)
      {:layout :column}))

(deftest covariance-matches-r-demo-matrix
  (testing "sample covariance matrix vs R cov() for 10×3 returns"
    (let [x (-> (demo-returns-matrix) cov/column-demean! cov/covariance-matrix)
          got (cov/matrix->row-vecs x)]
      (is (near-matrix? 1e-14 r-cov-rows got)))))

(deftest correlation-matches-r-demo-matrix
  (testing "Pearson correlation matrix vs R cor() for 10×3 returns"
    (let [r (cov/correlation-matrix (demo-returns-matrix))
          got (cov/matrix->row-vecs r)]
      (is (near-matrix? 1e-14 r-cor-rows got)))))
