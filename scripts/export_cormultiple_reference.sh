#!/usr/bin/env bash
# Regenerate scripts/data/cormultiple_*.csv used by scripts/cormultiple_r_verify.R
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/lib/math"
exec clojure -M:neanderthal -e "
(require '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[quanta.notebook.math.cormultiple :as cm]
         '[quanta.math.neanderthal :as nm])

(letfn [(write-csv-matrix [path row-vecs]
          (with-open [w (io/writer path)]
            (.write w (str/join \",\" (map #(str \"v\" (inc %)) (range (count (first row-vecs))))))
            (.write w \"\\n\")
            (doseq [row row-vecs]
              (.write w (str/join \",\" row))
              (.write w \"\\n\"))))
        (write-csv-vec [path v]
          (with-open [w (io/writer path)]
            (.write w (str/join \"\\n\" (map str v)))))]
  (let [R (fn [m] (nm/matrix->row-vecs m))
        ret (cm/random-returns-matrix 60 5 42)
        cors (let [c1 (nm/correlation-matrix (cm/tail-rows ret 5))
                   c3 (nm/correlation-matrix (cm/tail-rows ret 15))
                   c6 (nm/correlation-matrix (cm/tail-rows ret 30))
                   c12 (nm/correlation-matrix ret)]
               (cm/weight-correlation-matrices c1 c3 c6 c12))
        vols (cm/column-sample-stdevs (cm/tail-rows ret 5))
        covs (cm/covariance-from-cor-and-vols cors vols)
        root (str \"$ROOT/scripts/data/\") ]
    (.mkdirs (java.io.File. root))
    (write-csv-matrix (str root \"cormultiple_returns.csv\") (R ret))
    (write-csv-matrix (str root \"cormultiple_cors_clojure.csv\") (R cors))
    (write-csv-vec (str root \"cormultiple_vols_clojure.txt\") vols)
    (write-csv-matrix (str root \"cormultiple_covs_clojure.csv\") (R covs))
    (println (str \"Wrote reference CSVs under \" root))))
"
