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

import java.util.List;

import org.dmg.pmml.Constant;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Segmentation;
import org.jpmml.converter.Schema;
import org.jpmml.converter.MiningModelUtil;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;

public class LogisticRegression extends Regression {

	@Override
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, Schema schema){
		List<FieldName> activeFields = schema.getActiveFields();

		Output output = encodeOutput(base_score);

		MiningSchema miningSchema = ModelUtil.createMiningSchema(null, activeFields);

		MiningModel miningModel = new MiningModel(MiningFunctionType.REGRESSION, miningSchema)
			.setSegmentation(segmentation)
			.setOutput(output);

		return MiningModelUtil.createRegression(schema, miningModel);
	}

	private Output encodeOutput(float base_score){
		Output output = new Output();

		OutputField xgbValue = createPredictedField(output, base_score);

		Constant one = PMMLUtil.createConstant(1f);

		// "1 / (1 + exp(-1 * y))"
		Expression expression = PMMLUtil.createApply("/", one, PMMLUtil.createApply("+", one, PMMLUtil.createApply("exp", PMMLUtil.createApply("*", PMMLUtil.createConstant(-1f), new FieldRef(xgbValue.getName())))));

		OutputField transformedValue = createTransformedField(FieldName.create("transformedValue"), expression);

		output.addOutputFields(transformedValue);

		return output;
	}
}