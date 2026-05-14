# Verify R vs Clojure (`quanta.notebook.math.cormultiple`) on the same return sample.
#
# Sample returns live in scripts/data/cormultiple_returns.csv (60 rows x 5 assets).
# Clojure reference matrices were exported from the same generator:
#   seed 42, Java Random, Gaussian * 0.015, column-major fill (see notebook).
#
# Regenerate reference CSVs after changing Clojure sampling:
#   scripts/export_cormultiple_reference.sh
#
# Run (any cwd; paths resolve from this script):
#   Rscript /path/to/repo/scripts/cormultiple_r_verify.R

ca <- commandArgs(trailingOnly = FALSE)
file_arg <- grep("^--file=", ca, value = TRUE)
if (length(file_arg) != 1L) {
  stop("Expected Rscript --file=... (run: Rscript scripts/cormultiple_r_verify.R from repo root)",
       call. = FALSE)
}
script_path <- normalizePath(sub("^--file=", "", file_arg), mustWork = TRUE)
data_dir <- file.path(dirname(script_path), "data")

read_num_matrix_csv <- function(path) {
  m <- as.matrix(read.csv(path, check.names = FALSE))
  storage.mode(m) <- "double"
  dimnames(m) <- NULL
  m
}

returns_path <- file.path(data_dir, "cormultiple_returns.csv")
cors_clj_path <- file.path(data_dir, "cormultiple_cors_clojure.csv")
vols_clj_path <- file.path(data_dir, "cormultiple_vols_clojure.txt")
covs_clj_path <- file.path(data_dir, "cormultiple_covs_clojure.csv")

if (!file.exists(returns_path)) {
  stop("Missing ", returns_path,
       "\nExport reference data from Clojure first (see comments at top of this file).",
       call. = FALSE)
}

returns <- read_num_matrix_csv(returns_path)
stopifnot(dim(returns) == c(60L, 5L))

idx <- 1:5

one_month <- tail(returns, 5)
three_months <- tail(returns, 15)
six_months <- tail(returns, 30)
ret_subset <- returns

## `cors_mat`: same object as Clojure `cors-mat` / `fast-correlation-average-matrix`
cors_mat <- (
  cor(one_month[, idx, drop = FALSE]) * 12
  + cor(three_months[, idx, drop = FALSE]) * 4
  + cor(six_months[, idx, drop = FALSE]) * 2
  + cor(ret_subset[, idx, drop = FALSE])
) / 19

vols_r <- apply(one_month[, idx, drop = FALSE], 2, sd)

covs_r <- outer(vols_r, vols_r) * cors_mat

tol <- 1e-12

## Internal: covs built from this cors_mat (matches R `outer(vols)*cors`)
cmp_cors_cov <- all.equal(covs_r, outer(vols_r, vols_r) * cors_mat,
                          tolerance = tol, check.attributes = FALSE)
if (!isTRUE(cmp_cors_cov)) {
  stop("cors_mat vs covs_r consistency:\n", cmp_cors_cov, call. = FALSE)
}
message("OK: covs_r equals outer(vols_r, vols_r) * cors_mat (self-consistent).")

if (file.exists(cors_clj_path)) {
  cors_clj <- read_num_matrix_csv(cors_clj_path)
  cmp_cors <- all.equal(cors_mat, cors_clj, tolerance = tol, check.attributes = FALSE)
  if (!isTRUE(cmp_cors)) {
    stop("cors-mat mismatch vs Clojure export:\n", cmp_cors, call. = FALSE)
  }
  message("OK: cors-mat (weighted correlation) matches Clojure export (tol=", tol, ").")
} else {
  message("Skip cors-mat check (missing ", cors_clj_path, ").")
}

if (file.exists(vols_clj_path)) {
  vols_clj <- scan(vols_clj_path, quiet = TRUE)
  cmp_vols <- all.equal(as.numeric(vols_r), as.numeric(vols_clj),
                        tolerance = tol, check.attributes = FALSE)
  if (!isTRUE(cmp_vols)) {
    stop("vols mismatch:\n", cmp_vols, call. = FALSE)
  }
  message("OK: one-month column sample SDs match Clojure.")
} else {
  message("Skip vols check (missing ", vols_clj_path, ").")
}

if (file.exists(covs_clj_path)) {
  covs_clj <- read_num_matrix_csv(covs_clj_path)
  cmp_covs <- all.equal(covs_r, covs_clj, tolerance = tol, check.attributes = FALSE)
  if (!isTRUE(cmp_covs)) {
    stop("covs mismatch:\n", cmp_covs, call. = FALSE)
  }
  message("OK: covariance from outer(vols)*cors matches Clojure.")
} else {
  message("Skip covs check (missing ", covs_clj_path, ").")
}

message("All requested checks passed.")
