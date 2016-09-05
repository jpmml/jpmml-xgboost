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

import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class PoissonRegression extends Regression {

	@Override
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, Schema schema){
		Schema segmentSchema = schema.toAnonymousSchema();

		Output output = encodeOutput();

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(segmentSchema))
			.setSegmentation(segmentation)
			.setTargets(createTargets(base_score, segmentSchema))
			.setOutput(output);

		return MiningModelUtil.createRegression(schema, miningModel);
	}

	static
	private Output encodeOutput(){
		OutputField xgbValue = createPredictedField(FieldName.create("xgbValue"));

		// "exp(y)"
		Expression expression = PMMLUtil.createApply("exp", new FieldRef(xgbValue.getName()));

		OutputField transformedXgbValue = createTransformedField(FieldName.create("transformedXgbValue"), expression);

		Output output = new Output()
			.addOutputFields(xgbValue, transformedXgbValue);

		return output;
	}
}