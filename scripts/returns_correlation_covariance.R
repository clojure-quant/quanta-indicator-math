# Three assets, ten return observations per asset.
# Rows = time periods, columns = assets (R's cor/cov treat columns as variables).

set.seed(42)

returns <- matrix(
  c(
    0.01, -0.02,  0.00,  0.015, -0.005, 0.008, -0.012, 0.003,  0.006, -0.004,
    0.005,  0.01, -0.008, 0.002,  0.012, 0.004,  0.007, -0.009, 0.001,  0.011,
    -0.01,  0.003, 0.009, -0.006, 0.004, -0.011, 0.002,  0.01,  -0.003, 0.005
  ),
  nrow = 10,
  ncol = 3,
  dimnames = list(
    paste0("t", 1:10),
    c("asset_A", "asset_B", "asset_C")
  )
)

correlation_matrix <- cor(returns)
covariance_matrix  <- cov(returns)

cat("Return matrix (10 x 3):\n")
print(returns)

cat("\nCorrelation matrix:\n")
print(correlation_matrix)

cat("\nCovariance matrix (sample covariance, denominator n-1):\n")
print(covariance_matrix)
