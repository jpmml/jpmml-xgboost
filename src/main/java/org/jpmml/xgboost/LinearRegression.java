/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.xgboost;

import org.dmg.pmml.FeatureType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;

public class LinearRegression extends ObjFunction {

	public LinearRegression(){
	}

	@Override
	public Output encodeOutput(){
		OutputField xgbValue = new OutputField(FieldName.create("xgbValue"))
			.setFeature(FeatureType.PREDICTED_VALUE);

		Output output = new Output()
			.addOutputFields(xgbValue);

		return output;
	}
}