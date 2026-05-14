(ns quanta.math.neanderthal-test
  "Neanderthal helpers vs R reference CSVs in `test/quanta/math/rdata/`.

  **How the R reference CSVs are produced**
  From the repo root, run `scripts/gen_quanta_math_test_rdata.sh` (it calls
  `scripts/gen_quanta_math_test_rdata.R` with the repo root). The R script:
  - Draws an `m × p` matrix `X` (`set.seed(42)`), writes `returns.csv`, then
    `demeaned.csv`, `standardized.csv`, `covariance.csv`, `correlation.csv` as before.
  - **column_sample_stdevs.csv** — same `X` as `returns.csv`; one header row + one data
    row of `apply(X, 2, sd)` (R’s sample sd, divisor `n−1`), matching
    `column-sample-stdevs`.
  - **cormult_c1.csv … cormult_c12.csv** and **cormult_weighted.csv** — four independent
    synthetic return panels (`p_w = 4` columns, different row counts and seeds), each
    passed through `cor()`; then `W = (12*C1 + 4*C3 + 2*C6 + C12) / 19`, matching
    `weight-correlation-matrices`.
  - **covfromcor_cor.csv**, **covfromcor_vols.csv**, **covfromcor_cov.csv** — `cor()` of a
    `30 × 3` synthetic panel, a fixed length-3 volatility vector `c(0.024, 0.011, 0.031)`,
    and `Cov <- outer(vols, vols) * Cor` (element-wise), matching `covariance-from-cor-and-vols`."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [quanta.math.neanderthal :as nm]
   [uncomplicate.neanderthal.core :as n :refer [ge]]
   [uncomplicate.neanderthal.native :refer [native-double]]))

(def ^:private rdata-dir
  (str (System/getProperty "user.dir") "/test/quanta/math/rdata"))

(defn- slurp-csv-dense-matrix
  "Parse a CSV with a header row into row-major `rows` (each inner vec is one row)
   and dimensions `m` × `n`."
  [path]
  (let [lines (-> path io/file slurp str/split-lines)
        header (str/split (first lines) #",")
        n (count header)
        rows (mapv (fn [line]
                     (mapv #(Double/parseDouble %) (str/split line #",")))
                   (rest lines))
        m (count rows)]
    (when (zero? m)
      (throw (ex-info "empty CSV after header" {:path path})))
    (when-not (every? #(= n (count %)) rows)
      (throw (ex-info "ragged CSV rows" {:path path :n n})))
    {:m m :n n :rows rows}))

(defn- rows->col-major-doubles ^doubles [rows]
  (let [m (count rows)
        n (count (first rows))
        out (double-array (* m n))]
    (dotimes [j n]
      (dotimes [i m]
        (aset out (+ (* j m) i) (double (nth (nth rows i) j)))))
    out))

(defn- csv->ge [path]
  (let [{:keys [m n rows]} (slurp-csv-dense-matrix path)]
    (ge native-double m n (rows->col-major-doubles rows) {:layout :column})))

(defn- csv->first-data-row
  "First line after CSV header as a vector of doubles."
  [path]
  (let [lines (-> path io/file slurp str/split-lines)]
    (when (< (count lines) 2)
      (throw (ex-info "expected header + ≥1 data row" {:path path})))
    (mapv #(Double/parseDouble %) (str/split (second lines) #","))))

(defn- near-vec? [eps expected actual]
  (and (= (count expected) (count actual))
       (every? (fn [[e a]] (< (Math/abs (- e a)) eps))
                 (map vector expected actual))))

(defn- near-matrix? [eps expected-rows actual-rows]
  (every? (fn [[e a]] (< (Math/abs (- e a)) eps))
            (map vector (flatten expected-rows) (flatten actual-rows))))

(defn- columns->col-major-doubles ^doubles [cols]
  (let [m (count (first cols))
        n (count cols)
        out (double-array (* m n))]
    (dotimes [j n]
      (let [col (nth cols j)]
        (dotimes [i m]
          (aset out (+ (* j m) i) (double (nth col i))))))
    out))

;; Same numeric layout as R: matrix(..., nrow = 10, ncol = 3) fills by column.
(def ^:private demo-returns-cols
  '[[0.01 -0.02 0.0 0.015 -0.005 0.008 -0.012 0.003 0.006 -0.004]
    [0.005 0.01 -0.008 0.002 0.012 0.004 0.007 -0.009 0.001 0.011]
    [-0.01 0.003 0.009 -0.006 0.004 -0.011 0.002 0.01 -0.003 0.005]])

(def ^:private r-cov-rows
  [[0.000113211111111111 -3.33888888888889e-05 -4.33666666666667e-05]
   [-3.33888888888889e-05 5.36111111111111e-05 -1.49444444444444e-05]
   [-4.33666666666667e-05 -1.49444444444444e-05 5.55666666666667e-05]])

(def ^:private r-cor-rows
  [[1.0 -0.428578160402653 -0.546769578324926]
   [-0.428578160402653 1.0 -0.273807410652732]
   [-0.546769578324926 -0.273807410652732 1.0]])

(defn- demo-returns-matrix []
  (ge native-double 10 3 (columns->col-major-doubles demo-returns-cols)
      {:layout :column}))

(deftest column-demean-matches-r-csv
  (testing "column-demean! vs R sweep(X, 2, colMeans(X), '-')"
    (let [x (n/copy (csv->ge (str rdata-dir "/returns.csv")))
          _ (nm/column-demean! x)
          got (nm/matrix->row-vecs x)
          {:keys [rows]} (slurp-csv-dense-matrix (str rdata-dir "/demeaned.csv"))]
      (is (near-matrix? 1e-12 rows got)))))

(deftest covariance-matrix-matches-r-csv
  (testing "covariance-matrix after demean vs R cov(X)"
    (let [x (-> (csv->ge (str rdata-dir "/returns.csv")) n/copy nm/column-demean! nm/covariance-matrix)
          got (nm/matrix->row-vecs x)
          {:keys [rows]} (slurp-csv-dense-matrix (str rdata-dir "/covariance.csv"))]
      (is (near-matrix? 1e-12 rows got)))))

(deftest column-standardize-matches-r-csv
  (testing "demean + column-standardize! vs R scale(X, center=TRUE, scale=TRUE)"
    (let [x (-> (csv->ge (str rdata-dir "/returns.csv")) n/copy nm/column-demean! nm/column-standardize!)
          got (nm/matrix->row-vecs x)
          {:keys [rows]} (slurp-csv-dense-matrix (str rdata-dir "/standardized.csv"))]
      (is (near-matrix? 1e-12 rows got)))))

(deftest correlation-matrix-matches-r-csv
  (testing "correlation-matrix vs R cor(X)"
    (let [r (nm/correlation-matrix (csv->ge (str rdata-dir "/returns.csv")))
          got (nm/matrix->row-vecs r)
          {:keys [rows]} (slurp-csv-dense-matrix (str rdata-dir "/correlation.csv"))]
      (is (near-matrix? 1e-12 rows got)))))

(deftest column-sample-stdevs-matches-r-csv
  (testing "column-sample-stdevs vs R apply(X, 2, sd)"
    (let [x (csv->ge (str rdata-dir "/returns.csv"))
          got (nm/column-sample-stdevs x)
          want (csv->first-data-row (str rdata-dir "/column_sample_stdevs.csv"))]
      (is (near-vec? 1e-12 want got)))))

(deftest weight-correlation-matrices-matches-r-csv
  (testing "weight-correlation-matrices vs R (12*C1 + 4*C3 + 2*C6 + C12) / 19"
    (let [c1 (csv->ge (str rdata-dir "/cormult_c1.csv"))
          c3 (csv->ge (str rdata-dir "/cormult_c3.csv"))
          c6 (csv->ge (str rdata-dir "/cormult_c6.csv"))
          c12 (csv->ge (str rdata-dir "/cormult_c12.csv"))
          w (nm/weight-correlation-matrices c1 c3 c6 c12)
          got (nm/matrix->row-vecs w)
          {:keys [rows]} (slurp-csv-dense-matrix (str rdata-dir "/cormult_weighted.csv"))]
      (is (near-matrix? 1e-12 rows got)))))

(deftest covariance-from-cor-and-vols-matches-r-csv
  (testing "covariance-from-cor-and-vols vs R outer(vols, vols) * Cor"
    (let [cor-mat (csv->ge (str rdata-dir "/covfromcor_cor.csv"))
          vol-vec (csv->first-data-row (str rdata-dir "/covfromcor_vols.csv"))
          cov (nm/covariance-from-cor-and-vols cor-mat vol-vec)
          got (nm/matrix->row-vecs cov)
          {:keys [rows]} (slurp-csv-dense-matrix (str rdata-dir "/covfromcor_cov.csv"))]
      (is (near-matrix? 1e-12 rows got)))))

(deftest covariance-matrix-requires-two-rows
  (is (thrown? clojure.lang.ExceptionInfo
               (nm/covariance-matrix
                (ge native-double 1 2 (double-array [1.0 2.0]) {:layout :column})))))

(deftest covariance-matches-r-demo-matrix
  (testing "sample covariance matrix vs R cov() for 10×3 returns"
    (let [x (-> (demo-returns-matrix) nm/column-demean! nm/covariance-matrix)
          got (nm/matrix->row-vecs x)]
      (is (near-matrix? 1e-14 r-cov-rows got)))))

(deftest correlation-matches-r-demo-matrix
  (testing "Pearson correlation matrix vs R cor() for 10×3 returns"
    (let [r (nm/correlation-matrix (demo-returns-matrix))
          got (nm/matrix->row-vecs r)]
      (is (near-matrix? 1e-14 r-cor-rows got)))))
