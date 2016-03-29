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

import org.dmg.pmml.Constant;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.jpmml.converter.PMMLUtil;

public class LogisticRegression extends LinearRegression {

	public LogisticRegression(){
	}

	@Override
	public Output encodeOutput(){
		Constant one = PMMLUtil.createConstant(1f);

		Output output = new Output();

		OutputField xgbValue = createPredictedField(FieldName.create("xgbValue"));

		// "1 / (1 + exp(-1 * y))"
		Expression expression = PMMLUtil.createApply("/", one, PMMLUtil.createApply("+", one, PMMLUtil.createApply("exp", PMMLUtil.createApply("*", PMMLUtil.createConstant(-1f), new FieldRef(xgbValue.getName())))));

		OutputField transformedValue = createTransformedField(FieldName.create("transformedValue"), expression);

		output.addOutputFields(xgbValue, transformedValue);

		return output;
	}
}