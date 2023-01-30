JPMML-XGBoost [![Build Status](https://github.com/jpmml/jpmml-xgboost/workflows/maven/badge.svg)](https://github.com/jpmml/jpmml-xgboost/actions?query=workflow%3A%22maven%22)
=============

Java library and command-line application for converting [XGBoost](https://github.com/dmlc/xgboost) models to PMML.

# Prerequisites #

* Java 1.8 or newer.

# Features #

Supports all XGBoost versions 0.4 through 1.7(.2).

* Functionality:
  * Model data formats:
    * Binary (XGBoost 0.4 and newer)
    * JSON (XGBoost 1.3 and newer)
    * Universal Binary JSON (UBJ) (XGBoost 1.6 and newer)
  * Gradient boosters:
    * GBTree
    * DART
  * Feature maps
  * Split types:
    * Numeric (XGBoost 0.4 and newer)
    * Categorical, One-Hot-Encoding (OHE)-based (XGBoost 1.5 and newer)
    * Categorical, Set-based (XGBoost 1.6 and newer)
    * Missing values (XGBoost 0.4 and newer)
  * Objective functions:
    * Regression
    * Binary- and multi-class classification
    * Ranking
    * Survival Analysis
* Conversion options:
  * Truncation (`ntree_limit` aka `iteration_range` parameters)
  * Elimination of empty and constant trees
  * Tree rearrangements:
    * Compaction and flattening (reshaping deep binary trees into shallow multi-way trees)
    * Pruning
* Production quality:
  * Complete test coverage.
  * Fully compliant with [JPMML-Evaluator](https://github.com/jpmml/jpmml-evaluator) and [JPMML-Transpiler](https://github.com/jpmml/jpmml-transpiler) libraries

# Installation #

Enter the project root directory and build using [Apache Maven](https://maven.apache.org/):
```
mvn clean install
```

The build produces a library JAR file `pmml-xgboost/target/pmml-xgboost-1.7-SNAPSHOT.jar`, and an executable uber-JAR file `pmml-xgboost-example/target/pmml-xgboost-example-executable-1.7-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use XGBoost to train a model.
2. Save the model and the associated feature map to files in a local filesystem.
3. Use the JPMML-XGBoost command-line converter application to turn those two files to a PMML file.

### The XGBoost side of operations

Training a binary classification model using the [`Audit.csv](https://github.com/jpmml/jpmml-xgboost/blob/master/pmml-xgboost/src/test/resources/csv/Audit.csv) dataset.

#### R language

```R
library("r2pmml")
library("xgboost")

df = read.csv("Audit.csv", stringsAsFactors = TRUE)

# Three continuous features, followed by five categorical features
X = df[c("Age", "Hours", "Income", "Education", "Employment", "Gender", "Marital", "Occupation")]
y = df["Adjusted"]

audit.formula = formula("~ . - 1")
audit.frame = model.frame(audit.formula, data = X, na.action = na.pass)
# Define rules for binarizing categorical features into binary indicator features
audit.contrasts = lapply(X[sapply(X, is.factor)], contrasts, contrasts = FALSE)
# Perform binarization
audit.matrix = model.matrix(audit.formula, data = audit.frame, contrasts.arg = audit.contrasts)

# Generate feature map based on audit.frame (not audit.matrix), because data.frame holds richer column meta-information than matrix
audit.fmap = r2pmml::as.fmap(audit.frame)
r2pmml::write.fmap(audit.fmap, "Audit.fmap")

audit.xgb = xgboost(data = audit.matrix, label = as.matrix(y), objective = "binary:logistic", nrounds = 131)
xgb.save(audit.xgb, "XGBoostAudit.json")
```

#### Python language (Learning API)

Using an `Audit.fmap` feature map file (works with any XGBoost version):
```python
from sklearn2pmml.xgboost import make_feature_map
from xgboost import DMatrix

import pandas
import xgboost

df = pandas.read_csv("Audit.csv")

# Three continuous features, followed by five categorical features
X = df[["Age", "Hours", "Income", "Education", "Employment", "Gender", "Marital", "Occupation"]]
y = df["Adjusted"]

# Convert categorical features into binary indicator features
X = pandas.get_dummies(data = X, prefix_sep = "=", dtype = bool)

audit_fmap = make_feature_map(X, enable_categorical = False)
audit_fmap.save("Audit.fmap")

audit_dmatrix = DMatrix(data = X, label = y)

audit_xgb = xgboost.train(params = {"objective" : "binary:logistic"}, dtrain = audit_dmatrix, num_boost_round = 131)
audit_xgb.save_model("XGBoostAudit.json")
```

The same, but using an embedded feature map (works with XGBoost 1.5 and newer):
```python
from xgboost import DMatrix

import pandas
import xgboost

def to_fmap_type(dtype):
    # Continuous integers
    if dtype == "int":
        return "int"
    # Continuous floats
    elif dtype == "float":
        return "float"
    # Binary indicators (ie. 0/1 values) generated by pandas.get_dummies(X)
    elif dtype == "bool":
        return "i"
    else:
        raise ValueError(dtype)

df = pandas.read_csv("Audit.csv")

# Three continuous features, followed by five categorical features
X = df[["Age", "Hours", "Income", "Education", "Employment", "Gender", "Marital", "Occupation"]]
y = df["Adjusted"]

# Convert categorical features into binary indicator features
X = pandas.get_dummies(data = X, prefix_sep = "=", dtype = bool)

feature_names = X.columns.values
feature_types = [to_fmap_type(dtype) for dtype in X.dtypes]

# Constructing a DMatrix with explicit feature names and feature types
audit_dmatrix = DMatrix(data = X, label = y, feature_names = feature_names, feature_types = feature_types)

audit_xgb = xgboost.train(params = {"objective" : "binary:logistic"}, dtrain = audit_dmatrix, num_boost_round = 131)
audit_xgb.save_model("XGBoostAudit.json")
```

### The JPMML-XGBoost side of operations

Converting the model file `XGBoostAudit.json` together with the associated feature map file `Audit.fmap` to a PMML file `XGBoostAudit.pmml`:
```
java -jar pmml-xgboost-example/target/pmml-xgboost-example-executable-1.7-SNAPSHOT.jar --model-input XGBoostAudit.json --fmap-input Audit.fmap --target-name Adjusted --pmml-output XGBoostAudit.pmml
```

If the XGBoost model contains an embedded feature map, then the `--fmap-input` command-line option may be omitted.

Getting help:
```
java -jar pmml-xgboost-example/target/pmml-xgboost-example-executable-1.7-SNAPSHOT.jar --help
```

# Documentation #

* [Training Python-based XGBoost accelerated failure time (AFT) models](https://openscoring.io/blog/2023/01/28/python_xgboost_aft_pmml/)
* [One-hot-encoding (OHE) categorical features in Scikit-Learn based XGBoost pipelines](https://openscoring.io/blog/2022/04/12/onehot_encoding_sklearn_xgboost_pipeline/)
* [Training Scikit-Learn based TF(-IDF) plus XGBoost pipelines](https://openscoring.io/blog/2021/02/27/sklearn_tf_tfidf_xgboost_pipeline/)
* [Stacking Scikit-Learn, LightGBM and XGBoost models](https://openscoring.io/blog/2020/01/02/stacking_sklearn_lightgbm_xgboost/)
* [Extending Scikit-Learn with GBDT plus LR ensemble (GBDT+LR) model type](https://openscoring.io/blog/2019/06/19/sklearn_gbdt_lr_ensemble/) (Using XGBoost models on the GBDT side of GBDT+LR ensemble)

# License #

JPMML-XGBoost is licensed under the terms and conditions of the [GNU Affero General Public License, Version 3.0](https://www.gnu.org/licenses/agpl-3.0.html).

If you would like to use JPMML-XGBoost in a proprietary software project, then it is possible to enter into a licensing agreement which makes JPMML-XGBoost available under the terms and conditions of the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause) instead.

# Additional information #

JPMML-XGBoost is developed and maintained by Openscoring Ltd, Estonia.

Interested in using [Java PMML API](https://github.com/jpmml) software in your company? Please contact [info@openscoring.io](mailto:info@openscoring.io)
