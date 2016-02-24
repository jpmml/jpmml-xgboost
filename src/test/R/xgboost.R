library("xgboost")

source("../../main/R/util.R")

setwd("../resources/")

loadCsv = function(file){
	return (read.csv(file = file, header = TRUE))
}

storeCsv = function(data, file){
	write.table(data, file, sep = ",", quote = FALSE, row.names = FALSE)
}

#
# Auto$mpg
#

auto = loadCsv("csv/Auto.csv")
auto$cylinders = as.factor(auto$cylinders)
auto$origin = as.factor(auto$origin)

auto_X = auto[, 1:(ncol(auto) - 1)]
auto_y = auto[, ncol(auto)]

auto.fmap = genFMap(auto_X, "xgboost/Auto.fmap")
auto.dmatrix = genDMatrix(auto_y, auto_X, "csv/Auto.svm")

set.seed(42)

auto.xgb = xgboost(data = auto.dmatrix, objective = "reg:linear", nrounds = 15)
xgb.save(auto.xgb, "xgboost/LinearRegressionAuto.model")
xgb.dump(auto.xgb, "xgboost/LinearRegressionAuto.txt", fmap = "xgboost/Auto.fmap")

storeCsv(data.frame("xgbValue" = predict(auto.xgb, newdata = auto.dmatrix)), "csv/LinearRegressionAuto.csv")

#
# Audit$Adjusted
#

audit = loadCsv("csv/Audit.csv")
audit$Deductions = NULL

audit_X = audit[, 1:(ncol(audit) - 1)]
audit_y = audit[, ncol(audit)]

audit.fmap = genFMap(audit_X, "xgboost/Audit.fmap")
audit.dmatrix = genDMatrix(audit_y, audit_X, "csv/Audit.svm")

set.seed(42)

audit.xgb = xgboost(data = audit.dmatrix, objective = "reg:logistic", nrounds = 15)
xgb.save(audit.xgb, "xgboost/LogisticRegressionAudit.model")
xgb.dump(audit.xgb, "xgboost/LogisticRegressionAudit.txt", fmap = "xgboost/Audit.fmap")

storeCsv(data.frame("transformedValue" = predict(audit.xgb, newdata = audit.dmatrix)), "csv/LogisticRegressionAudit.csv")
