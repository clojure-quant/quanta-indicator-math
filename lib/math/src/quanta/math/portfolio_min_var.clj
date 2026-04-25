(ns quanta.math.portfolio-min-var
  (:require 
   [quanta.math.covariance :as cov])
  (:import
   (org.ojalgo.optimisation ExpressionsBasedModel Variable Optimisation$Result)))


(defn min-var-portfolio
  "Solve a minimum-variance long-only portfolio with box constraints.

   Inputs:
   - cov: NxN covariance matrix as nested vectors
   - lower-bound: scalar lower bound for each weight
   - upper-bound: scalar upper bound for each weight

   Returns:
   {:weights [...]
    :variance ...
    :state ...}"
  [cov lower-bound upper-bound]
  (let [n (count cov)
        model (ExpressionsBasedModel.)
        ;; create variables w0 ... w(n-1)
        vars (vec
              (for [i (range n)]
                (doto (.newVariable model (str "w" i))
                  (.lower (double lower-bound))
                  (.upper (double upper-bound))
                  (.weight 0.0))))]

    ;; sum(weights) = 1
    (let [expr (.addExpression model "budget")]
      (doseq [^Variable v vars]
        (.set expr v 1.0))
      (.level expr 1.0))

    ;; quadratic objective: w' Σ w
    ;;
    ;; We put Σ into the quadratic factors of an expression and assign
    ;; expression weight 1.0 so ojAlgo minimizes that quadratic form.
    (let [risk (.addExpression model "risk")]
      (doseq [i (range n)
              j (range n)]
        (.set
         risk
         ^Variable (vars i)
         ^Variable (vars j)
         (double (get-in cov [i j]))))
      (.weight risk 1.0))

    ;; solve
    (let [^Optimisation$Result result (.minimise model)
          weights (mapv #(.doubleValue result (int %)) (range n))
          ;; compute realized variance directly in Clojure
          variance (reduce +
                           (for [i (range n)
                                 j (range n)]
                             (* (weights i)
                                (get-in cov [i j])
                                (weights j))))]
      {:weights weights
       :variance variance
       :state (str (.getState result))})))


;; Sector cap
;;  If assets 0 and 1 are tech, enforce tech ≤ 50%:

#_(let [expr (.addExpression model "tech-cap")]
  (.set expr ^Variable (vars 0) 1.0)
  (.set expr ^Variable (vars 1) 1.0)
  (.upper expr 0.50))

(defn return-ds->min-var-portfolio [returns-ds assets min-weight max-weight]
  (-> (cov/ds->covariance-matrix returns-ds assets)
      cov/matrix->row-vecs
      (min-var-portfolio min-weight max-weight)
      (assoc :assets assets)))
