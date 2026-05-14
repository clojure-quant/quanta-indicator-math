# Generate reference CSVs for Clojure `quanta.math.neanderthal-test` (neanderthal_test.clj).
# Output: `lib/math/test/quanta/math/rdata/`.
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

message("Wrote reference CSVs to:\n  ", normalizePath(out_dir, winslash = "/", mustWork = TRUE))
