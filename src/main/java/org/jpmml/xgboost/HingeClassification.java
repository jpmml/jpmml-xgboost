/*
 * Copyright (c) 2020 Villu Ruusmann
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

import org.dmg.pmml.Apply;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.FunctionTransformation;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.Transformation;
import org.jpmml.converter.mining.MiningModelUtil;

public class HingeClassification extends Classification {

	public HingeClassification(){
		super(2);
	}

	@Override
	public MiningModel encodeMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, boolean numeric, Schema schema){
		Schema segmentSchema = schema.toAnonymousRegressorSchema(DataType.FLOAT);

		Transformation transformation = new FunctionTransformation(PMMLFunctions.THRESHOLD){

			@Override
			public FieldName getName(FieldName name){
				return FieldNameUtil.create("hinge", name);
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				Apply apply = (Apply)super.createExpression(fieldRef);

				apply.addExpressions(PMMLUtil.createConstant(0f));

				return apply;
			}
		};

		MiningModel miningModel = createMiningModel(trees, weights, base_score, ntreeLimit, numeric, segmentSchema)
			.setOutput(ModelUtil.createPredictedOutput(FieldName.create("xgbValue"), OpType.CONTINUOUS, DataType.FLOAT, transformation));

		return MiningModelUtil.createBinaryLogisticClassification(miningModel, 1d, 0d, RegressionModel.NormalizationMethod.NONE, true, schema);
	}
}