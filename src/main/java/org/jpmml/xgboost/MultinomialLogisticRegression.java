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

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.CMatrixUtil;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class MultinomialLogisticRegression extends Classification {

	public MultinomialLogisticRegression(int num_class){
		super(num_class);

		if(num_class < 3){
			throw new IllegalArgumentException("Multi-class classification requires three or more target categories");
		}
	}

	@Override
	public MiningModel encodeMiningModel(List<RegTree> regTrees, float base_score, Integer ntreeLimit, Schema schema){
		Schema segmentSchema = new Schema(new ContinuousLabel(null, DataType.FLOAT), schema.getFeatures());

		List<MiningModel> miningModels = new ArrayList<>();

		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

		for(int i = 0, columns = categoricalLabel.size(), rows = (regTrees.size() / columns); i < columns; i++){
			MiningModel miningModel = createMiningModel(CMatrixUtil.getColumn(regTrees, rows, columns, i), base_score, ntreeLimit, segmentSchema)
				.setOutput(ModelUtil.createPredictedOutput(FieldName.create("xgbValue(" + categoricalLabel.getValue(i) + ")"), OpType.CONTINUOUS, DataType.FLOAT));

			miningModels.add(miningModel);
		}

		return MiningModelUtil.createClassification(miningModels, RegressionModel.NormalizationMethod.SOFTMAX, true, schema);
	}
}