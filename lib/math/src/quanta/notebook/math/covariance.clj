(ns quanta.notebook.math.covariance
  (:require
   [quanta.math.covariance :as cov]
   [quanta.notebook.math.demo-data :refer [returns-ds]]
   [quanta.math.portfolio-min-var :refer [min-var-portfolio return-ds->min-var-portfolio]]
   ))

(def cm (cov/ds->covariance-matrix returns-ds ["AAPL" "MSFT" "GOOG"]))

(cov/matrix->row-vecs cm)

;; printing is not showing enough digits.
(println cm)




(return-ds->min-var-portfolio returns-ds ["AAPL" "MSFT" "GOOG"]  0.0 0.4)

(return-ds->min-var-portfolio returns-ds ["AAPL" "MSFT" "GOOG"]  0.0 0.5)

(return-ds->min-var-portfolio returns-ds ["AAPL" "MSFT" "GOOG"]  0.0 0.7)

(return-ds->min-var-portfolio returns-ds ["AAPL" "MSFT" "GOOG"]  0.0 0.8)

(return-ds->min-var-portfolio returns-ds ["AAPL" "MSFT" "GOOG"]  0.0 0.9)

(return-ds->min-var-portfolio returns-ds ["AAPL" "MSFT" "GOOG"]  0.0 1.0)



 