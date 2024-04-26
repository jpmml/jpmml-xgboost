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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class BinomialLogisticRegression extends Classification {

	public BinomialLogisticRegression(String name){
		super(name, 2);
	}

	@Override
	public float probToMargin(float value){
		return inverseLogit(value);
	}

	@Override
	public MiningModel encodeMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, Schema schema){
		Schema segmentSchema = schema.toAnonymousRegressorSchema(DataType.FLOAT);

		MiningModel miningModel = createMiningModel(trees, weights, base_score, ntreeLimit, segmentSchema)
			.setOutput(ModelUtil.createPredictedOutput("xgbValue", OpType.CONTINUOUS, DataType.FLOAT));

		return MiningModelUtil.createBinaryLogisticClassification(miningModel, 1d, 0d, RegressionModel.NormalizationMethod.LOGIT, true, schema);
	}
}