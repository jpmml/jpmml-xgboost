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

import com.google.common.collect.Lists;
import org.dmg.pmml.DataField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Segmentation;
import org.jpmml.converter.MiningModelUtil;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.ValueUtil;

public class LogisticClassification extends Classification {

	public LogisticClassification(){
		super(2);
	}

	@Override
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, FeatureMap featureMap){
		DataField dataField = getDataField();

		FieldName targetField = dataField.getName();

		List<FieldName> activeFields = PMMLUtil.getNames(featureMap.getDataFields());

		MiningSchema miningSchema = ModelUtil.createMiningSchema(null, activeFields);

		Output output;

		FieldName xgbField;

		if(!ValueUtil.isZero(base_score)){
			OutputField rawXgbValue = createPredictedField(FieldName.create("rawXgbValue"));

			Expression expression = PMMLUtil.createApply("+", new FieldRef(rawXgbValue.getName()), PMMLUtil.createConstant(base_score));

			OutputField scaledXgbValue = createTransformedField(FieldName.create("scaledXgbValue"), expression);

			output = new Output()
				.addOutputFields(rawXgbValue, scaledXgbValue);

			xgbField = scaledXgbValue.getName();
		} else

		{
			OutputField xgbValue = createPredictedField(FieldName.create("xgbValue"));

			output = new Output()
				.addOutputFields(xgbValue);

			xgbField = xgbValue.getName();
		}

		MiningModel miningModel = new MiningModel(MiningFunctionType.REGRESSION, miningSchema)
			.setSegmentation(segmentation)
			.setOutput(output);

		return MiningModelUtil.createBinaryLogisticClassification(targetField, Lists.reverse(getTargetCategories()), activeFields, miningModel, xgbField, -1d, true);
	}
}