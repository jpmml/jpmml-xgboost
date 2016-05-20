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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Output;
import org.dmg.pmml.Segmentation;
import org.jpmml.converter.FeatureSchema;
import org.jpmml.converter.MiningModelUtil;
import org.jpmml.converter.ModelUtil;

public class LogisticClassification extends Classification {

	public LogisticClassification(){
		super(2);
	}

	@Override
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, FeatureSchema schema){
		List<FieldName> activeFields = schema.getActiveFields();

		MiningSchema miningSchema = ModelUtil.createMiningSchema(null, activeFields);

		Output output = encodeOutput(base_score);

		MiningModel miningModel = new MiningModel(MiningFunctionType.REGRESSION, miningSchema)
			.setSegmentation(segmentation)
			.setOutput(output);

		return MiningModelUtil.createBinaryLogisticClassification(schema, miningModel, -1d, true);
	}

	private Output encodeOutput(float base_score){
		Output output = new Output();

		createPredictedField(output, base_score);

		return output;
	}
}