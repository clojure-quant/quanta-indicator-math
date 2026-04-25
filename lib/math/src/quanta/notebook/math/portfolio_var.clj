(ns quanta.notebook.math.portfolio-var
  (:require
   [quanta.math.portfolio-min-var :refer [min-var-portfolio]]))

(def cov
  [[0.040 0.018 0.012 0.010]
   [0.018 0.090 0.015 0.011]
   [0.012 0.015 0.025 0.009]
   [0.010 0.011 0.009 0.030]])

(min-var-portfolio cov 0.0 0.4)

;; A typical result would look like:

{:weights [0.31 0.07 0.34 0.28]
 :variance 0.0218
 :state "OPTIMAL"}

