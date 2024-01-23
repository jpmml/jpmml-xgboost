# 1.8.2

## Breaking changes

None

## New features

* Added support for [One-Model-Per-Target (OMPT)](https://xgboost.readthedocs.io/en/stable/tutorials/multioutput.html#training-with-one-model-per-target)-style multi-target models:
  * Regression
  * Binary classification

See https://github.com/jpmml/jpmml-xgboost/issues/70

* Improved support for random forest-style models.

XGBoost boosting and bagging model files are structurally identical.

The two model types can be told apart from one another by observing the value of the `/learner/gradient_booster/gbtree/model/gbtree_model_param/num_parallel_tree` integer attribute.
Boosting models have it set to `1`, whereas bagging models (eg. random forests) have it set to the number of member decision trees models in the ensemble model,

All the member decision tree models are stored in a `gbtree/model/trees` array attribute.
When dealing with OMPT-style multi-target models, then the model converter must partition the contents of this attribute into multiple sub-arrays, one for each target.

Currently, the partitioning algorithm is a little shaky.
Integration tests confirm its correctness for "pure boosting" and "pure bagging" sub-types. However, it may yield incorrect results for the mixed "boosted bagging" sub-type.

* Improved support for early stopping.

If the training process was halted using the early stopping mechanism, then this is indicated by setting the `best_iteration` and `best_score` attributes on the native booster object.

XGBoost 1.X versions tended to duplicate this information at the Python wrapper object level.
For example, in the form of a `xgboost.core.Booster.best_ntree_limit` attribute.

XGBoost 2.X versions no longer do this.
The converter must now check for the early stopping status by loading the native booster object and querying its `/learner/attributes/best_iteration` and `/learner/attributes/best_score` attributes.

## Minor improvements and fixes

* Updated integration testing resources to XGBoost 2.0.3
