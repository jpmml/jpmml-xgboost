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

Using [`r2pmml`](https://github.com/jpmml/r2pmml) and [`xgboost`](https://cran.r-project.org/web/packages/xgboost/) packages to train a regression model for the example `mtcars` dataset:
```R
library("r2pmml")
library("xgboost")

data(mtcars)

# Convert selected columns from numeric datatype to integer or factor datatypes
mtcars$cyl = as.integer(mtcars$cyl)
mtcars$vs = as.factor(mtcars$vs)
mtcars$am = as.factor(mtcars$am)
mtcars$gear = as.integer(mtcars$gear)
mtcars$carb = as.integer(mtcars$carb)

mtcars_y = mtcars[, 1]
mtcars_X = mtcars[, 2:ncol(mtcars)]

mtcars.formula = formula(~ . - 1)
mtcars.frame = model.frame(mtcars.formula, data = mtcars_X)
mtcars.matrix = model.matrix(mtcars.formula, data = mtcars.frame)

# Generate feature map
mtcars.fmap = as.fmap(mtcars.frame)
write.fmap(mtcars.fmap, "xgboost.fmap")

# Generate DMatrix
mtcars.dmatrix = xgb.DMatrix(data = mtcars.matrix, label = mtcars_y)

set.seed(31)

# Train a linear regression model
mtcars.xgb = xgboost(data = mtcars.dmatrix, objective = "reg:squarederror", nrounds = 17)

# Save the model in XGBoost proprietary binary format
xgb.save(mtcars.xgb, "xgboost.model")

# Dump the model in text format
xgb.dump(mtcars.xgb, "xgboost.model.txt", fmap = "xgboost.fmap")
```

### The JPMML-XGBoost side of operations

Converting the model file `xgboost.model` together with the associated feature map file `xgboost.fmap` to a PMML file `xgboost.pmml`:
```
java -jar pmml-xgboost-example/target/pmml-xgboost-example-executable-1.6-SNAPSHOT.jar --model-input xgboost.model --fmap-input xgboost.fmap --target-name mpg --pmml-output xgboost.pmml
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
