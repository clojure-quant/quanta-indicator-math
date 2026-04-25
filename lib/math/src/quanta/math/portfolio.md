The best choices I found are:

Neanderthal
Use this for the heavy linear algebra: covariance matrices, matrix products, factor models, etc. It is a fast Clojure matrix library built on BLAS/LAPACK, with CPU and GPU backends.

ojAlgo (via Java interop)
This is probably the most practical choice for Markowitz / mean-variance style optimization with constraints. ojAlgo supports LP, QP, and MIP on the JVM, and its QP support is the natural fit for quadratic portfolio problems. It also separates the model from the solver, which makes it nice to call from Clojure.

jcobyla (via Java interop)
Good when your portfolio objective or constraints are nonlinear and you want a derivative-free solver. It supports inequality constraints only and is aimed at moderate-size nonlinear problems.

provisdom/solvers
A real Clojure library that wraps several solver backends. Its README says it is built on Hipparchus, ojAlgo, and jcobyla, and it includes linear programming with equality / ≤ / ≥ constraints.

matlib
Useful if you want a more Clojure-friendly layer on top of Neanderthal. It includes optimization methods like L-BFGS, gradient descent, and differential evolution. That can be helpful for custom objective functions, though it is not specifically a portfolio package.

A useful caution: Hipparchus alone is not great for constrained parameter optimization in the way portfolio problems often need. Its own docs note that least-squares solvers currently do not allow parameter constraints.

My practical recommendation:

For classic portfolio optimization with constraints like
weights sum to 1
long-only
per-asset min/max weights
sector caps
turnover bounds encoded linearly
use ojAlgo + Neanderthal.
For custom nonlinear constraints/objectives
use jcobyla, or possibly matlib if you are implementing more of the optimization logic yourself.
If you want to stay closer to pure Clojure APIs first, check provisdom/solvers, but for serious mean-variance work I would still expect ojAlgo interop to be the main engine.

So the short list is:

Neanderthal: matrix math
ojAlgo: best general solver choice for constrained portfolio QP
jcobyla: nonlinear inequality-constrained optimization
provisdom/solvers: Clojure wrapper toolkit around multiple solvers
matlib: higher-level numerical/optimization helpers on top of Neanderthal

If you want, I can give you a Clojure example of minimum-variance portfolio optimization with box constraints and sum-to-1 using ojAlgo.