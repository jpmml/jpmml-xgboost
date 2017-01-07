#install.packages("drat", repos="https://cran.rstudio.com")
#drat:::addRepo("dmlc")
#remove.packages("xgboost")
#install.packages("xgboost", repos="http://dmlc.ml/drat/", type="source")

library("r2pmml")
library("xgboost")

setwd("../resources/")

loadCsv = function(file){
	return (read.csv(file = file, header = TRUE))
}

storeCsv = function(data, file){
	write.table(data, file, sep = ",", quote = FALSE, row.names = FALSE, col.names = gsub("X_target", "_target", names(data)))
}

csvFile = function(name, ext){
	return (paste("csv/", name, ext, sep = ""))
}

xgboostFile = function(name, ext){
	return (paste("xgboost/", name, ext, sep = ""))
}

# See http://stackoverflow.com/a/27454361/1808924
insertNA = function(df){
	mod = function(x){
		return (x[sample(c(TRUE, NA), prob = c(0.75, 0.25), size = length(x), replace = TRUE)])
	}

	df = as.data.frame(lapply(df, FUN = mod))

	return (df)
}

#
# Regression
#

genAutoMpg = function(auto_y, auto_X, dataset){
	auto.fmap = genFMap(auto_X)
	writeFMap(auto.fmap, csvFile(dataset, ".fmap"))

	auto.dmatrix = genDMatrix(auto_y, auto_X, csvFile(dataset, ".svm"))

	funcAndDataset = paste("LinearRegression", dataset, sep = "")

	set.seed(42)

	auto.xgb = xgboost(data = auto.dmatrix, objective = "reg:linear", nrounds = 15)
	xgb.save(auto.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(auto.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	storeCsv(data.frame("_target" = predict(auto.xgb, newdata = auto.dmatrix)), csvFile(funcAndDataset, ".csv"))
}

auto = loadCsv("csv/Auto.csv")
auto$cylinders = as.factor(auto$cylinders)
auto$origin = as.factor(auto$origin)

auto_y = auto[, ncol(auto)]
auto_X = auto[, 1:(ncol(auto) - 1)]

genAutoMpg(auto_y, auto_X, "Auto")

set.seed(31)

auto_X = insertNA(auto_X)

storeCsv(cbind(auto_X, "mpg" = auto_y), "csv/AutoNA.csv")

genAutoMpg(auto_y, auto_X, "AutoNA")

#
# Poisson regression
#

genVisitCount = function(visit_y, visit_X, dataset){
	visit.fmap = genFMap(visit_X)
	writeFMap(visit.fmap, csvFile(dataset, ".fmap"))

	visit.dmatrix = genDMatrix(visit_y, visit_X, csvFile(dataset, ".svm"))

	funcAndDataset = paste("PoissonRegression", dataset, sep = "")

	set.seed(42)

	visit.xgb = xgboost(data = visit.dmatrix, objective = "count:poisson", nrounds = 15)
	xgb.save(visit.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(visit.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	storeCsv(data.frame("_target" = predict(visit.xgb, newdata = visit.dmatrix)), csvFile(funcAndDataset, ".csv"))
}

visit = loadCsv("csv/Visit.csv")

visit_y = visit[, ncol(visit)]
visit_X = visit[, 1:(ncol(visit) - 1)]

genVisitCount(visit_y, visit_X, "Visit")

set.seed(31)

visit_X = insertNA(visit_X)

storeCsv(cbind(visit_X, "docvis" = visit_y), "csv/VisitNA.csv")

genVisitCount(visit_y, visit_X, "VisitNA")

#
# Binary classification
#

genAuditAdjusted = function(audit_y, audit_X, dataset){
	audit.fmap = genFMap(audit_X)
	writeFMap(audit.fmap, csvFile(dataset, ".fmap"))

	audit.dmatrix = genDMatrix(audit_y, audit_X, csvFile(dataset, ".svm"))

	funcAndDataset = paste("LogisticRegression", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.dmatrix, objective = "reg:logistic", nrounds = 11)
	xgb.save(audit.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(audit.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	adjusted = predict(audit.xgb, newdata = audit.dmatrix)

	storeCsv(data.frame("_target" = adjusted), csvFile(funcAndDataset, ".csv"))

	funcAndDataset = paste("LogisticClassification", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.dmatrix, objective = "binary:logistic", nrounds = 15)
	xgb.save(audit.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(audit.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	probability_1 = predict(audit.xgb, newdata = audit.dmatrix)
	probability_0 = (1 - probability_1)

	storeCsv(data.frame("_target" = as.integer(probability_1 > 0.5), "probability_0" = probability_0, "probability_1" = probability_1), csvFile(funcAndDataset, ".csv"))
}

audit = loadCsv("csv/Audit.csv")
audit$Deductions = NULL

audit_y = audit[, ncol(audit)]
audit_X = audit[, 1:(ncol(audit) - 1)]

genAuditAdjusted(audit_y, audit_X, "Audit")

set.seed(31)

audit_X = insertNA(audit_X)

storeCsv(cbind(audit_X, "Adjusted" = audit_y), "csv/AuditNA.csv")

genAuditAdjusted(audit_y, audit_X, "AuditNA")

#
# Multi-class classification
#

genIrisSpecies = function(iris_y, iris_X, dataset){
	iris.fmap = genFMap(iris_X)
	writeFMap(iris.fmap, csvFile(dataset, ".fmap"))

	iris.dmatrix = genDMatrix(iris_y, iris_X, csvFile(dataset, ".svm"))

	funcAndDataset = paste("SoftMaxClassification", dataset, sep = "")

	set.seed(42)

	iris.xgb = xgboost(data = iris.dmatrix, objective = "multi:softprob", num_class = 3, nrounds = 15)
	xgb.save(iris.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(iris.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	probabilities = predict(iris.xgb, newdata = iris.dmatrix)

	# Convert from vector to three-column matrix
	probabilities = t(matrix(probabilities, 3, nrow(iris_X)))

	species = unlist(apply(probabilities, 1, FUN = function(x){ (which.max(x) - 1) }), use.names = FALSE)

	storeCsv(data.frame("_target" = species, "probability_0" = probabilities[, 1], "probability_1" = probabilities[, 2], "probability_2" = probabilities[, 3]), csvFile(funcAndDataset, ".csv"))
}

iris = loadCsv("csv/Iris.csv")

iris_y = iris[, ncol(iris)]
iris_X = iris[, 1:(ncol(iris) - 1)]

# Convert from factor levels to 0-based indexes
iris_y = (as.integer(iris_y) - 1)

genIrisSpecies(iris_y, iris_X, "Iris")

set.seed(31)

iris_X = insertNA(iris_X)

storeCsv(cbind(iris_X, "Species" = iris$Species), "csv/IrisNA.csv")

genIrisSpecies(iris_y, iris_X, "IrisNA")