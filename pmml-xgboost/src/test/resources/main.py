from pandas import DataFrame
from sklearn.preprocessing import LabelEncoder
from sklearn2pmml.xgboost import make_feature_map
from xgboost.core import XGBoostError

import numpy
import pandas
import sys
import xgboost

datasets = []

if __name__ == "__main__":
	if len(sys.argv) > 1:
		datasets = (sys.argv[1]).split(",")
	else:
		datasets = ["Audit", "Auto", "Iris", "Lung", "Visit"]

def csv_file(name, ext):
	return "csv/" + name + ext;

def xgboost_file(name, ext):
	return "xgboost/" + name + ext;

def load_csv(path):
	return pandas.read_csv(path)

def split_csv(df):
	columns = df.columns.tolist()
	return (df[columns[: -1]], df[columns[-1]])

def load_split_csv(dataset):
	df = load_csv(csv_file(dataset, ".csv"))
	return split_csv(df)

def store_csv(df, path):
	df.to_csv(path, sep = ",", header = True, index = False)

def store_model(booster, algorithm, dataset, with_legacy_binary = True, with_json = True, with_ubjson = True):
	if with_legacy_binary:
		booster.save_model(xgboost_file(algorithm + dataset, ".model"))

	if with_json:
		booster.save_model(xgboost_file(algorithm + dataset, ".json"))

	if with_ubjson:
		booster.save_model(xgboost_file(algorithm + dataset, ".ubj"))

	# XXX
	#booster.dump_model(xgboost_file(algorithm + dataset, ".dump"), fmap = csv_file(dataset, ".fmap"), dump_format = "json")

def store_result(df, name):
	store_csv(df, csv_file(name, ".csv"))

def make_opts(num_rounds = None):
	return {"iteration_range" : (0, num_rounds)} if num_rounds else {}

#
# Survival regression
#

def predict_lung(lung_booster, lung_dmat):
	time = lung_booster.predict(lung_dmat)

	result = DataFrame(time, columns = ["_target"])

	return result

def train_lung(dataset, **params):
	lung_df = load_csv(csv_file(dataset, ".csv"))

	lung_X = lung_df[[col for col in lung_df.columns.tolist() if col not in ["time", "status"]]]
	lung_y = lung_df[["time", "status"]]

	lung_fmap = make_feature_map(lung_X)
	lung_fmap.save(csv_file(dataset, ".fmap"))

	lung_y_lower = lung_y["time"].copy()

	lung_y_upper = lung_y["time"].copy()
	lung_y_upper[lung_y["status"] == 0] = numpy.inf

	lung_dmat = xgboost.DMatrix(data = lung_X, label_lower_bound = lung_y_lower, label_upper_bound = lung_y_upper)

	lung_params = dict(**params)
	lung_params.update({
		"objective" : "survival:aft",
		"eval_metric" : "aft-nloglik",
		"aft_loss_distribution" : "normal",
		"max_depth" : 2,
		"seed" : 42
	})

	lung_booster = xgboost.train(params = lung_params, dtrain = lung_dmat, num_boost_round = 15)
	store_model(lung_booster, "AFT", dataset)

	store_csv(predict_lung(lung_booster, lung_dmat), csv_file("AFT" + dataset, ".csv"))

if "Lung" in datasets:
	train_lung("LungNA")

#
# Regression
#

def predict_auto(auto_booster, auto_dmat, num_rounds = None):
	mpg = auto_booster.predict(auto_dmat, **make_opts(num_rounds))

	result = DataFrame(mpg, columns = ["_target"])

	return result

def train_auto(dataset, **params):
	auto_X, auto_y = load_split_csv(dataset)

	for col in ["cylinders", "model_year", "origin"]:
		auto_X[col] = auto_X[col].astype("Int64").astype("category")

	auto_fmap = make_feature_map(auto_X, category_to_indicator = True)
	auto_fmap.save(csv_file(dataset, ".fmap"))

	auto_dmat = xgboost.DMatrix(data = auto_X, label = auto_y, enable_categorical = True)

	auto_params = dict(**params)
	auto_params.update({
		"objective" : "reg:squarederror",
		"tree_method" : "hist",
		"seed" : 42
	})

	auto_booster = xgboost.train(params = auto_params, dtrain = auto_dmat, num_boost_round = 31)
	store_model(auto_booster, "LinearRegression", dataset, with_legacy_binary = False)

	store_csv(predict_auto(auto_booster, auto_dmat), csv_file("LinearRegression" + dataset, ".csv"))

if "Auto" in datasets:
	train_auto("Auto", booster = "dart", rate_drop = 0.05)
	train_auto("AutoNA")

def predict_visit(visit_booster, visit_dmat, num_rounds = None):
	count = visit_booster.predict(visit_dmat, **make_opts(num_rounds))

	result = DataFrame(count, columns = ["_target"])

	return result

def train_visit(dataset, **params):
	visit_X, visit_y = load_split_csv(dataset)

	# categorical strings
	for col in ["edlevel"]:
		visit_X[col] = visit_X[col].astype("category")

	# categorical integers
	for col in ["female", "kids", "married", "outwork", "self"]:
		visit_X[col] = visit_X[col].astype("Int64").astype("category")

	visit_fmap = make_feature_map(visit_X, category_to_indicator = True)
	visit_fmap.save(csv_file(dataset, ".fmap"))

	visit_dmat = xgboost.DMatrix(data = visit_X, label = visit_y, enable_categorical = True)

	visit_params = dict(**params)
	visit_params.update({
		"objective" : "count:poisson",
		"tree_method" : "hist",
		"seed" : 42
	})

	visit_booster = xgboost.train(params = visit_params, dtrain = visit_dmat, num_boost_round = 31)
	store_model(visit_booster, "PoissonRegression", dataset, with_legacy_binary = False)

	store_csv(predict_visit(visit_booster, visit_dmat), csv_file("PoissonRegression" + dataset, ".csv"))

	visit_params.update({
		"objective" : "reg:gamma"
	})

	visit_booster = xgboost.train(params = visit_params, dtrain = visit_dmat, num_boost_round = 31)
	store_model(visit_booster, "GammaRegression", dataset, with_legacy_binary = False)

	store_csv(predict_visit(visit_booster, visit_dmat), csv_file("GammaRegression" + dataset, ".csv"))

	visit_params.update({
		"objective" : "reg:tweedie",
		"tweedie_variance_power" : 1.5 
	})

	visit_booster = xgboost.train(params = visit_params, dtrain = visit_dmat, num_boost_round = 31)
	store_model(visit_booster, "TweedieRegression", dataset, with_legacy_binary = False)

	store_csv(predict_visit(visit_booster, visit_dmat), csv_file("TweedieRegression" + dataset, ".csv"))

if "Visit" in datasets:
	train_visit("Visit", booster = "dart", rate_drop = 0.05)
	train_visit("VisitNA")

#
# Binary classification
#

def predict_audit(audit_booster, audit_dmat, num_rounds = None):
	adjusted = audit_booster.predict(audit_dmat, **make_opts(num_rounds))

	result = DataFrame(adjusted, columns = ["_target"])

	return result

def predict_binomial_audit(audit_booster, audit_dmat, num_rounds = None):
	adjusted_proba = audit_booster.predict(audit_dmat, **make_opts(num_rounds)).reshape((-1, 1))

	result = DataFrame(numpy.hstack(((adjusted_proba > 0.5), (1 - adjusted_proba), adjusted_proba)), columns = ["_target", "probability(0)", "probability(1)"])
	result["_target"] = result["_target"].astype(int)

	return result

def predict_multinomial_audit(audit_booster, audit_dmat, num_rounds = None):
	adjusted_proba = audit_booster.predict(audit_dmat, **make_opts(num_rounds))
	adjusted = numpy.argmax(adjusted_proba, axis = 1)

	result = DataFrame(numpy.hstack((adjusted.reshape(-1, 1), adjusted_proba)), columns = ["_target", "probability(0)", "probability(1)"])
	result["_target"] = result["_target"].astype(int)

	return result

def train_audit(dataset, **params):
	audit_X, audit_y = load_split_csv(dataset)

	for col in ["Education", "Employment", "Gender", "Marital", "Occupation"]:
		audit_X[col] = audit_X[col].astype("category")

	audit_y = audit_y.astype(int)

	audit_fmap = make_feature_map(audit_X, category_to_indicator = True)
	audit_fmap.save(csv_file(dataset, ".fmap"))

	audit_dmat = xgboost.DMatrix(data = audit_X, label = audit_y, enable_categorical = True)

	audit_params = dict(**params)
	audit_params.update({
		"objective" : "reg:logistic",
		"tree_method" : "hist",
		"seed" : 42
	})

	audit_booster = xgboost.train(params = audit_params, dtrain = audit_dmat, num_boost_round = 17)
	store_model(audit_booster, "LogisticRegression", dataset, with_legacy_binary = False)

	store_csv(predict_audit(audit_booster, audit_dmat), csv_file("LogisticRegression" + dataset, ".csv"))

	audit_params.update({
		"objective" : "binary:logistic"
	})

	audit_booster = xgboost.train(params = audit_params, dtrain = audit_dmat, num_boost_round = 71)
	store_model(audit_booster, "BinomialClassification", dataset, with_legacy_binary = False)

	store_csv(predict_binomial_audit(audit_booster, audit_dmat), csv_file("BinomialClassification" + dataset, ".csv"))
	store_csv(predict_binomial_audit(audit_booster, audit_dmat, 31), csv_file("BinomialClassification" + dataset + "@31", ".csv"))

	audit_params.update({
		"objective" : "binary:hinge"
	})

	audit_booster = xgboost.train(params = audit_params, dtrain = audit_dmat, num_boost_round = 31)
	store_model(audit_booster, "HingeClassification", dataset, with_legacy_binary = False)

	store_csv(predict_binomial_audit(audit_booster, audit_dmat), csv_file("HingeClassification" + dataset, ".csv"))

	audit_params.update({
		"objective" : "multi:softprob",
		"num_class" : 2
	})

	audit_booster = xgboost.train(params = audit_params, dtrain = audit_dmat, num_boost_round = 31)
	store_model(audit_booster, "MultinomialClassification", dataset, with_legacy_binary = False)

	store_csv(predict_multinomial_audit(audit_booster, audit_dmat), csv_file("MultinomialClassification" + dataset, ".csv"))

if "Audit" in datasets:
	train_audit("Audit", booster = "dart", rate_drop = 0.05)
	train_audit("AuditNA")

#
# Multi-class classification
#

def predict_iris(iris_booster, iris_dmat, num_rounds = None):
	species_proba = iris_booster.predict(iris_dmat, **make_opts(num_rounds))
	species = numpy.argmax(species_proba, axis = 1)

	result = DataFrame(numpy.hstack((species.reshape(-1, 1), species_proba)), columns = ["_target", "probability(0)", "probability(1)", "probability(2)"])
	result["_target"] = result["_target"].astype(int)

	return result

def train_iris(dataset, **params):
	iris_X, iris_y = load_split_csv(dataset)

	iris_le = LabelEncoder()
	iris_y = iris_le.fit_transform(iris_y)

	iris_fmap = make_feature_map(iris_X)
	iris_fmap.save(csv_file(dataset, ".fmap"))

	iris_dmat = xgboost.DMatrix(data = iris_X, label = iris_y)

	iris_params = dict(**params)
	iris_params.update({
		"objective" : "multi:softprob", 
		"num_class" : 3,
		"seed" : 42
	})

	iris_booster = xgboost.train(params = iris_params, dtrain = iris_dmat, num_boost_round = 17)
	store_model(iris_booster, "MultinomialClassification", dataset)

	store_csv(predict_iris(iris_booster, iris_dmat), csv_file("MultinomialClassification" + dataset, ".csv"))
	store_csv(predict_iris(iris_booster, iris_dmat, 11), csv_file("MultinomialClassification" + dataset + "@11", ".csv"))

if "Iris" in datasets:
	train_iris("Iris", booster = "dart", rate_drop = 0.05)
	train_iris("IrisNA")
