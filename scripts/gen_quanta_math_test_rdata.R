# Generate reference CSVs for Clojure `quanta.math.neanderthal-test` (neanderthal_test.clj).
# Output: `lib/math/test/quanta/math/rdata/`.
#
# Also writes R reference data for:
#   - column-sample-stdevs  → column_sample_stdevs.csv (apply(X, 2, sd), same X as returns.csv)
#   - weight-correlation-matrices → cormult_c{1,3,6,12}.csv + cormult_weighted.csv
#   - covariance-from-cor-and-vols → covfromcor_cor.csv, covfromcor_vols.csv, covfromcor_cov.csv
#
# From repo root:
#   Rscript scripts/gen_quanta_math_test_rdata.R
#
# Or with explicit repo root (first argument):
#   Rscript scripts/gen_quanta_math_test_rdata.R /path/to/quanta-indicator-math

args <- commandArgs(trailingOnly = TRUE)
if (length(args) >= 1L) {
  repo_root <- normalizePath(args[[1L]], mustWork = TRUE)
} else {
  ca <- commandArgs(trailingOnly = FALSE)
  fa <- grep("^--file=", ca, value = TRUE)
  if (length(fa) != 1L) {
    stop("Run from repo root: Rscript scripts/gen_quanta_math_test_rdata.R\n",
         "Or pass repo root as first argument.",
         call. = FALSE)
  }
  script_path <- normalizePath(sub("^--file=", "", fa[[1L]]), mustWork = TRUE)
  repo_root <- dirname(dirname(script_path))
}

out_dir <- file.path(repo_root, "lib", "math", "test", "quanta", "math", "rdata")
dir.create(out_dir, recursive = TRUE, showWarnings = FALSE)

set.seed(42L)
m <- 18L
p <- 5L
X <- matrix(rnorm(as.integer(m * p), mean = 0, sd = 0.02), nrow = m, ncol = p)
colnames(X) <- paste0("v", seq_len(p))

write.csv(X, file.path(out_dir, "returns.csv"), row.names = FALSE, quote = FALSE)

Xd <- sweep(X, 2L, colMeans(X), "-")
write.csv(Xd, file.path(out_dir, "demeaned.csv"), row.names = FALSE, quote = FALSE)

Xs <- scale(X, center = TRUE, scale = TRUE)
storage.mode(Xs) <- "double"
write.csv(Xs, file.path(out_dir, "standardized.csv"), row.names = FALSE, quote = FALSE)

Cov <- cov(X)
write.csv(Cov, file.path(out_dir, "covariance.csv"), row.names = FALSE, quote = FALSE)

Cor <- cor(X)
write.csv(Cor, file.path(out_dir, "correlation.csv"), row.names = FALSE, quote = FALSE)

# ---- column-sample-stdevs (matches Clojure: per-column sd with divisor n-1) ----
# Same matrix X as returns.csv — R `apply(X, 2, sd)` uses sample sd.
st <- apply(X, 2L, sd)
dim(st) <- c(1L, p)
colnames(st) <- paste0("v", seq_len(p))
write.csv(st, file.path(out_dir, "column_sample_stdevs.csv"), row.names = FALSE, quote = FALSE)

# ---- weight-correlation-matrices: (12*C1 + 4*C3 + 2*C6 + C12) / 19 ----
# Four synthetic return panels (same ncol), each `cor()` = Pearson sample correlation.
write_cor_csv <- function(M, path) {
  storage.mode(M) <- "double"
  colnames(M) <- paste0("v", seq_len(ncol(M)))
  write.csv(M, path, row.names = FALSE, quote = FALSE)
}

p_w <- 4L
mkR <- function(nr, seed) {
  set.seed(as.integer(seed))
  matrix(rnorm(as.integer(nr * p_w), mean = 0, sd = 0.03), nrow = nr, ncol = p_w)
}

C1 <- cor(mkR(22L, 501L))
C3 <- cor(mkR(25L, 502L))
C6 <- cor(mkR(28L, 503L))
C12 <- cor(mkR(35L, 504L))
Wcor <- (12 * C1 + 4 * C3 + 2 * C6 + C12) / 19

write_cor_csv(C1, file.path(out_dir, "cormult_c1.csv"))
write_cor_csv(C3, file.path(out_dir, "cormult_c3.csv"))
write_cor_csv(C6, file.path(out_dir, "cormult_c6.csv"))
write_cor_csv(C12, file.path(out_dir, "cormult_c12.csv"))
write_cor_csv(Wcor, file.path(out_dir, "cormult_weighted.csv"))

# ---- covariance-from-cor-and-vols: Cov_ij = vol_i * vol_j * Cor_ij ----
# R: `outer(v, v) * Cor` (element-wise), same as `diag(v) %*% Cor %*% diag(v)`.
set.seed(777L)
p_cv <- 3L
Rret <- matrix(rnorm(as.integer(30L * p_cv), mean = 0, sd = 0.02), nrow = 30L, ncol = p_cv)
Cor_cv <- cor(Rret)
vols_cv <- c(0.024, 0.011, 0.031)
Cov_cv <- outer(vols_cv, vols_cv) * Cor_cv

write_cor_csv(Cor_cv, file.path(out_dir, "covfromcor_cor.csv"))
volm <- matrix(vols_cv, nrow = 1L)
colnames(volm) <- paste0("v", seq_len(p_cv))
write.csv(volm, file.path(out_dir, "covfromcor_vols.csv"), row.names = FALSE, quote = FALSE)
write_cor_csv(Cov_cv, file.path(out_dir, "covfromcor_cov.csv"))

message("Wrote reference CSVs to:\n  ", normalizePath(out_dir, winslash = "/", mustWork = TRUE))
