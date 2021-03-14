library("Matrix")
library("r2pmml")
library("xgboost")

csvFile = function(name, ext){
	return (paste("csv/", name, ext, sep = ""))
}

xgboostFile = function(name, ext){
	return (paste("xgboost/", name, ext, sep = ""))
}

loadCsv = function(file){
	return (read.csv(file = file, header = TRUE))
}

storeCsv = function(data, file){
	write.table(data, file, sep = ",", quote = FALSE, row.names = FALSE, col.names = gsub("X_target", "_target", names(data)))
}

storeModel = function(xgb, funcAndDataset, dataset){
	xgb.save(xgb, xgboostFile(funcAndDataset, ".model"))
	xgb.save(xgb, xgboostFile(funcAndDataset, ".json"))

	xgb.dump(xgb, xgboostFile(funcAndDataset, ".dump"), fmap = csvFile(dataset, ".fmap"), dump_format = "json")
}

storeResult = function(data, funcAndDataset){
	storeCsv(data, csvFile(funcAndDataset, ".csv"))
}

# See http://stackoverflow.com/a/27454361
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

predictAutoMpg = function(auto.xgb, auto.matrix, ntreelimit){
	mpg = predict(auto.xgb, newdata = auto.matrix, ntreelimit = ntreelimit)

	return (data.frame("_target" = mpg))
}

genAutoMpg = function(auto_y, auto_X, dataset, ...){
	auto.formula = formula(~ . - 1)
	auto.frame = model.frame(auto.formula, data = auto_X, na.action = na.pass)
	auto.contrasts = lapply(auto_X[sapply(auto_X, is.factor)], contrasts, contrasts = FALSE)
	auto.matrix = model.matrix(auto.formula, data = auto.frame, contrasts.arg = auto.contrasts)
	auto.matrix = Matrix(auto.matrix, sparse = TRUE)

	auto.fmap = as.fmap(auto.frame)
	write.fmap(auto.fmap, csvFile(dataset, ".fmap"))

	funcAndDataset = paste("LinearRegression", dataset, sep = "")

	set.seed(42)

	auto.xgb = xgboost(data = auto.matrix, label = auto_y, objective = "reg:squarederror", nrounds = 31, ...)

	storeModel(auto.xgb, funcAndDataset, dataset)
	storeResult(predictAutoMpg(auto.xgb, auto.matrix, 31), funcAndDataset)
}

auto = loadCsv("csv/Auto.csv")
auto$cylinders = as.factor(auto$cylinders)
auto$origin = as.factor(auto$origin)

auto_y = auto[, ncol(auto)]
auto_X = auto[, 1:(ncol(auto) - 1)]

genAutoMpg(auto_y, auto_X, "Auto", booster = "dart", rate_drop = 0.05)

set.seed(31)

auto_X = insertNA(auto_X)

storeCsv(cbind(auto_X, "mpg" = auto_y), "csv/AutoNA.csv")

genAutoMpg(auto_y, auto_X, "AutoNA")

#
# Poisson, Tweedie and Gamma regressions
#

predictVisitCount = function(visit.xgb, visit.matrix, ntreelimit){
	count = predict(visit.xgb, newdata = visit.matrix, ntreelimit = ntreelimit)

	return (data.frame("_target" = count))
}

genVisitCount = function(visit_y, visit_X, dataset, ...){
	visit.formula = formula(~ . - 1)
	visit.frame = model.frame(visit.formula, data = visit_X, na.action = na.pass)
	visit.contrasts = lapply(visit_X[sapply(visit_X, is.factor)], contrasts, contrasts = FALSE)
	visit.matrix = model.matrix(visit.formula, data = visit.frame, contrasts.arg = visit.contrasts)
	visit.matrix = Matrix(visit.matrix, sparse = TRUE)

	visit.fmap = as.fmap(visit.frame)
	write.fmap(visit.fmap, csvFile(dataset, ".fmap"))

	funcAndDataset = paste("PoissonRegression", dataset, sep = "")

	set.seed(42)

	visit.xgb = xgboost(data = visit.matrix, label = visit_y, objective = "count:poisson", nrounds = 31, ...)

	storeModel(visit.xgb, funcAndDataset, dataset)
	storeResult(predictVisitCount(visit.xgb, visit.matrix, 31), funcAndDataset)

	funcAndDataset = paste("TweedieRegression", dataset, sep = "")

	set.seed(42);

	visit.dmatrix = xgb.DMatrix(data = visit.matrix, label = visit_y)

	visit.xgb = xgb.train(data = visit.dmatrix, params = list(objective = "reg:tweedie", tweedie_variance_power = 1.5), nrounds = 31)

	storeModel(visit.xgb, funcAndDataset, dataset)
	storeResult(predictVisitCount(visit.xgb, visit.matrix, 31), funcAndDataset)

	funcAndDataset = paste("GammaRegression", dataset, sep = "")

	set.seed(42)

	visit.xgb = xgboost(data = visit.matrix, label = visit_y, objective = "reg:gamma", nrounds = 31, ...)

	storeModel(visit.xgb, funcAndDataset, dataset)
	storeResult(predictVisitCount(visit.xgb, visit.matrix, 31), funcAndDataset)
}

visit = loadCsv("csv/Visit.csv")
visit$outwork = as.factor(visit$outwork)
visit$female = as.factor(visit$female)
visit$married = as.factor(visit$married)
visit$kids = as.factor(visit$kids)
visit$self = as.factor(visit$self)

visit_y = visit[, ncol(visit)]
visit_X = visit[, 1:(ncol(visit) - 1)]

genVisitCount(visit_y, visit_X, "Visit", booster = "dart", rate_drop = 0.05)

set.seed(31)

visit_X = insertNA(visit_X)

storeCsv(cbind(visit_X, "docvis" = visit_y), "csv/VisitNA.csv")

genVisitCount(visit_y, visit_X, "VisitNA")

#
# Binary classification
#

predictBinomialAuditAdjusted = function(audit.xgb, audit.matrix, ntreelimit){
	probability = predict(audit.xgb, newdata = audit.matrix, ntreelimit = ntreelimit)

	return (data.frame("_target" = as.integer(probability > 0.5), "probability(0)" = (1 - probability), "probability(1)" = probability, check.names = FALSE))
}

predictMultinomialAuditAdjusted = function(audit.xgb, audit.matrix, ntreelimit){
	probability = predict(audit.xgb, newdata = audit.matrix, ntreelimit = ntreelimit, reshape = TRUE)

	return (data.frame("_target" = as.integer(probability[, 2] > 0.5), "probability(0)" = probability[, 1], "probability(1)" = probability[, 2], check.names = FALSE))
}

genAuditAdjusted = function(audit_y, audit_X, dataset, ...){
	audit.formula = formula(~ . - 1)
	audit.frame = model.frame(audit.formula, data = audit_X, na.action = na.pass)
	audit.contrasts = lapply(audit_X[sapply(audit_X, is.factor)], contrasts, contrasts = FALSE)
	audit.matrix = model.matrix(audit.formula, data = audit.frame, contrasts.arg = audit.contrasts)
	audit.matrix = Matrix(audit.matrix, sparse = TRUE)

	audit.fmap = as.fmap(audit.frame)
	write.fmap(audit.fmap, csvFile(dataset, ".fmap"))

	funcAndDataset = paste("LogisticRegression", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.matrix, label = audit_y, objective = "reg:logistic", nrounds = 17, ...)

	adjusted = predict(audit.xgb, newdata = audit.matrix, ntreelimit = 17)

	storeModel(audit.xgb, funcAndDataset, dataset)
	storeResult(data.frame("_target" = adjusted), funcAndDataset)

	funcAndDataset = paste("BinomialClassification", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.matrix, label = audit_y, objective = "binary:logistic", nrounds = 71, ...)

	storeModel(audit.xgb, funcAndDataset, dataset)
	storeResult(predictBinomialAuditAdjusted(audit.xgb, audit.matrix, 71), funcAndDataset)
	storeResult(predictBinomialAuditAdjusted(audit.xgb, audit.matrix, 31), paste(funcAndDataset, "31", sep = "@"))

	funcAndDataset = paste("HingeClassification", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.matrix, label = audit_y, objective = "binary:hinge", nrounds = 31, ...)

	storeModel(audit.xgb, funcAndDataset, dataset)
	storeResult(predictBinomialAuditAdjusted(audit.xgb, audit.matrix, 31), funcAndDataset)

	funcAndDataset = paste("MultinomialClassification", dataset, sep = "")

	set.seed(42)

	audit.xgb = xgboost(data = audit.matrix, label = audit_y, objective = "multi:softprob", num_class = 2, nrounds = 31, ...)
	storeModel(audit.xgb, funcAndDataset, dataset)
	storeResult(predictMultinomialAuditAdjusted(audit.xgb, audit.matrix, 31), funcAndDataset)
}

audit = loadCsv("csv/Audit.csv")
audit$Deductions = NULL

audit_y = audit[, ncol(audit)]
audit_X = audit[, 1:(ncol(audit) - 1)]

genAuditAdjusted(audit_y, audit_X, "Audit", booster = "dart", rate_drop = 0.05)

set.seed(31)

audit_X = insertNA(audit_X)

storeCsv(cbind(audit_X, "Adjusted" = audit_y), "csv/AuditNA.csv")

genAuditAdjusted(audit_y, audit_X, "AuditNA")

#
# Multi-class classification
#

predictIrisSpecies = function(iris.xgb, iris.matrix, ntreelimit){
	probabilities = predict(iris.xgb, newdata = iris.matrix, ntreelimit = ntreelimit)

	# Convert from vector to three-column matrix
	probabilities = t(matrix(probabilities, 3, 150))

	species = unlist(apply(probabilities, 1, FUN = function(x){ (which.max(x) - 1) }), use.names = FALSE)

	return (data.frame("_target" = species, "probability(0)" = probabilities[, 1], "probability(1)" = probabilities[, 2], "probability(2)" = probabilities[, 3], check.names = FALSE))
}

genIrisSpecies = function(iris_y, iris_X, dataset, ...){
	iris.formula = formula(~ . - 1)
	iris.frame = model.frame(iris.formula, data = iris_X, na.action = na.pass)
	iris.matrix = model.matrix(iris.formula, data = iris.frame)
	iris.matrix = Matrix(iris.matrix, sparse = TRUE)

	iris.fmap = as.fmap(iris.frame)
	write.fmap(iris.fmap, csvFile(dataset, ".fmap"))

	funcAndDataset = paste("MultinomialClassification", dataset, sep = "")

	set.seed(42)

	iris.xgb = xgboost(data = iris.matrix, label = iris_y, objective = "multi:softprob", num_class = 3, nrounds = 17, ...)

	storeModel(iris.xgb, funcAndDataset, dataset)
	storeResult(predictIrisSpecies(iris.xgb, iris.matrix, 17), funcAndDataset)
	storeResult(predictIrisSpecies(iris.xgb, iris.matrix, 11), paste(funcAndDataset, "11", sep = "@"))
}

iris = loadCsv("csv/Iris.csv")

iris_y = iris[, ncol(iris)]
iris_X = iris[, 1:(ncol(iris) - 1)]

# Convert from factor levels to 0-based indexes
iris_y = (as.integer(iris_y) - 1)

genIrisSpecies(iris_y, iris_X, "Iris", booster = "dart", rate_drop = 0.05)

set.seed(31)

iris_X = insertNA(iris_X)

storeCsv(cbind(iris_X, "Species" = iris$Species), "csv/IrisNA.csv")

genIrisSpecies(iris_y, iris_X, "IrisNA")
