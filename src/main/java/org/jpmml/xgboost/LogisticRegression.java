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
import org.dmg.pmml.DataType;
import org.dmg.pmml.FeatureType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.jpmml.converter.PMMLUtil;

public class LogisticRegression extends ObjFunction {

	public LogisticRegression(){
	}

	@Override
	public Output encodeOutput(){
		Constant one = PMMLUtil.createConstant(1f);

		Output output = new Output();

		OutputField xgbValue = new OutputField(FieldName.create("xgbValue"))
			.setFeature(FeatureType.PREDICTED_VALUE);

		// "1 / (1 + exp(-1 * y))"
		OutputField objectiveValue = new OutputField(FieldName.create("transformedValue"))
			.setFeature(FeatureType.TRANSFORMED_VALUE)
			.setDataType(DataType.FLOAT)
			.setOpType(OpType.CONTINUOUS)
			.setExpression(PMMLUtil.createApply("/", one, PMMLUtil.createApply("+", one, PMMLUtil.createApply("exp", PMMLUtil.createApply("*", PMMLUtil.createConstant(-1), new FieldRef(xgbValue.getName()))))));

		output.addOutputFields(xgbValue, objectiveValue);

		return output;
	}
}