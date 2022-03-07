JPMML-XGBoost [![Build Status](https://github.com/jpmml/jpmml-xgboost/workflows/maven/badge.svg)](https://github.com/jpmml/jpmml-xgboost/actions?query=workflow%3A%22maven%22)
=============

Java library and command-line application for converting [XGBoost](https://github.com/dmlc/xgboost) models to PMML.

# Prerequisites #

* Java 1.8 or newer.

# Installation #

Enter the project root directory and build using [Apache Maven](https://maven.apache.org/):
```
mvn clean install
```

The build produces a library JAR file `pmml-xgboost/target/pmml-xgboost-1.6-SNAPSHOT.jar`, and an executable uber-JAR file `pmml-xgboost-example/target/pmml-xgboost-example-executable-1.6-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use XGBoost to train a model.
2. Save the model and the associated feature map to files in a local filesystem.
3. Use the JPMML-XGBoost command-line converter application to turn those two files to a PMML file.

### The XGBoost side of operations

Training a binary classification model using the [`Audit.csv](https://github.com/jpmml/jpmml-xgboost/blob/master/pmml-xgboost/src/test/resources/csv/Audit.csv) dataset.

##### R language

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

##### Python language (Learning API)

```python
from pandas import DataFrame
from xgboost import DMatrix

import pandas
import xgboost

def make_fmap(X):
    cols = X.columns
    dtypes = X.dtypes

    def to_fmap_type(dtype):
        if dtype == "int":
            return "int"
        elif dtype == "float":
            return "q"
        elif dtype == "uint8":
            return "i"
        else:
            raise ValueError(dtype)

    fmap_rows = []

    for col in cols:
        fmap_rows.append((len(fmap_rows), col, to_fmap_type(dtypes[col])))

    return DataFrame(fmap_rows, columns = ["id", "name", "type"])

def save_fmap(fmap, path):
    fmap.to_csv(path, header = False, index = False, sep = "\t")

df = pandas.read_csv("Audit.csv")

# Three continuous features, followed by five categorical features
X = df[["Age", "Hours", "Income", "Education", "Employment", "Gender", "Marital", "Occupation"]]
y = df["Adjusted"]

# Convert categorical features into binary indicator features
X = pandas.get_dummies(data = X, prefix_sep = "=")

audit_fmap = make_fmap(X)
save_fmap(audit_fmap, "Audit.fmap")

audit_dmatrix = DMatrix(data = X, label = y)

audit_xgb = xgboost.train(params = {"objective" : "binary:logistic"}, dtrain = audit_dmatrix, num_boost_round = 131)
audit_xgb.save_model("XGBoostAudit.json")
```

### The JPMML-XGBoost side of operations

Converting the model file `XGBoostAudit.json` together with the associated feature map file `Audit.fmap` to a PMML file `XGBoostAudit.pmml`:
```
java -jar pmml-xgboost-example/target/pmml-xgboost-example-executable-1.6-SNAPSHOT.jar --model-input XGBoostAudit.json --fmap-input Audit.fmap --target-name Adjusted --pmml-output XGBoostAudit.pmml
```

Getting help:
```
java -jar pmml-xgboost-example/target/pmml-xgboost-example-executable-1.6-SNAPSHOT.jar --help
```

# Documentation #

* [Training Scikit-Learn based TF(-IDF) plus XGBoost pipelines](https://openscoring.io/blog/2021/02/27/sklearn_tf_tfidf_xgboost_pipeline/)
* [Stacking Scikit-Learn, LightGBM and XGBoost models](https://openscoring.io/blog/2020/01/02/stacking_sklearn_lightgbm_xgboost/)
* [Extending Scikit-Learn with GBDT plus LR ensemble (GBDT+LR) model type](https://openscoring.io/blog/2019/06/19/sklearn_gbdt_lr_ensemble/) (Using XGBoost models on the GBDT side of GBDT+LR ensemble)

# License #

JPMML-XGBoost is licensed under the terms and conditions of the [GNU Affero General Public License, Version 3.0](https://www.gnu.org/licenses/agpl-3.0.html).

If you would like to use JPMML-XGBoost in a proprietary software project, then it is possible to enter into a licensing agreement which makes JPMML-XGBoost available under the terms and conditions of the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause) instead.

# Additional information #

JPMML-XGBoost is developed and maintained by Openscoring Ltd, Estonia.

Interested in using [Java PMML API](https://github.com/jpmml) software in your company? Please contact [info@openscoring.io](mailto:info@openscoring.io)
