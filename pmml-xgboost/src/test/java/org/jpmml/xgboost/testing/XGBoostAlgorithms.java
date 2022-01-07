/*
 * Copyright (c) 2021 Villu Ruusmann
 *
 * This file is part of JPMML-XGBoost
 *
 * JPMML-XGBoost is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-XGBoost is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-XGBoost.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.xgboost.testing;

public interface XGBoostAlgorithms {

	String BINOMIAL_CLASSIFICATION = "BinomialClassification";
	String GAMMA_REGRESSION = "GammaRegression";
	String HINGE_CLASSIFICATION = "HingeClassification";
	String LINEAR_REGRESSION = "LinearRegression";
	String LOGISTIC_REGRESSION = "LogisticRegression";
	String MULTINOMIAL_CLASSIFICATION = "MultinomialClassification";
	String POISSON_REGRESSION = "PoissonRegression";
	String TWEEDIE_REGRESSION = "TweedieRegression";
}