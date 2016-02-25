library("xgboost")

source("../../main/R/util.R")

setwd("../resources/")

loadCsv = function(file){
	return (read.csv(file = file, header = TRUE))
}

storeCsv = function(data, file){
	write.table(data, file, sep = ",", quote = FALSE, row.names = FALSE)
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
# Auto$mpg
#

genAutoMpg = function(auto_y, auto_X, dataset){
	auto.fmap = genFMap(auto_X, csvFile(dataset, ".fmap"))
	auto.dmatrix = genDMatrix(auto_y, auto_X, csvFile(dataset, ".svm"))

	funcAndDataset = paste("LinearRegression", dataset, sep = "")

	set.seed(42)

	auto.xgb = xgboost(data = auto.dmatrix, objective = "reg:linear", nrounds = 15)
	xgb.save(auto.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(auto.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	storeCsv(data.frame("xgbValue" = predict(auto.xgb, newdata = auto.dmatrix)), csvFile(funcAndDataset, ".csv"))
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
# Audit$Adjusted
#

genAuditAdjusted = function(audit_y, audit_X, dataset){
	audit.fmap = genFMap(audit_X, csvFile(dataset, ".fmap"))
	audit.dmatrix = genDMatrix(audit_y, audit_X, csvFile(dataset, ".svm"))

	funcAndDataset = paste("LogisticRegression", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.dmatrix, objective = "reg:logistic", nrounds = 15)
	xgb.save(audit.xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.dump(audit.xgb, xgboostFile(funcAndDataset, ".txt"), fmap = csvFile(dataset, ".fmap"))

	storeCsv(data.frame("transformedValue" = predict(audit.xgb, newdata = audit.dmatrix)), csvFile(funcAndDataset, ".csv"))
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
