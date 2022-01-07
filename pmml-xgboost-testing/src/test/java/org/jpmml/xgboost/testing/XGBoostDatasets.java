/*
 * Copyright (c) 2022 Villu Ruusmann
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

import org.jpmml.converter.testing.Datasets;

public interface XGBoostDatasets extends Datasets {

	String AUDIT_LIMIT = AUDIT + "@31";
	String AUDIT_NA_LIMIT = AUDIT_NA + "@31";
	String IRIS_LIMIT = IRIS + "@11";
	String IRIS_NA_LIMIT = IRIS_NA + "@11";
}