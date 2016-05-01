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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.MultipleModelMethodType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.RegressionNormalizationMethodType;
import org.dmg.pmml.Segment;
import org.dmg.pmml.Segmentation;
import org.jpmml.converter.MiningModelUtil;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;

public class SoftMaxClassification extends Classification {

	public SoftMaxClassification(int num_class){
		super(num_class);
	}

	@Override
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, FeatureMap featureMap){
		DataField dataField = getDataField();

		List<Segment> segments = segmentation.getSegments();

		MiningSchema valueMiningSchema = ModelUtil.createMiningSchema(null, featureMap.getDataFields());

		List<MiningModel> models = new ArrayList<>();

		List<String> targetCategories = getTargetCategories();
		for(int i = 0; i < targetCategories.size(); i++){
			String targetCategory = targetCategories.get(i);

			OutputField xgbValue = createPredictedField(FieldName.create("xgbValue_" + targetCategory));

			Expression expression = PMMLUtil.createApply("exp", PMMLUtil.createApply("+", new FieldRef(xgbValue.getName()), PMMLUtil.createConstant(base_score)));

			OutputField transformedValue = createTransformedField(FieldName.create("transformedValue_" + targetCategory), expression);

			List<Segment> valueSegments = getColumn(segments, i, (segments.size() / targetCategories.size()), targetCategories.size());

			Segmentation valueSegmentation = new Segmentation(MultipleModelMethodType.SUM, valueSegments);

			Output valueOutput = new Output()
				.addOutputFields(xgbValue, transformedValue);

			MiningModel valueMiningModel = new MiningModel(MiningFunctionType.REGRESSION, valueMiningSchema)
				.setSegmentation(valueSegmentation)
				.setOutput(valueOutput);

			models.add(valueMiningModel);
		}

		Function<DataField, FieldName> function = new Function<DataField, FieldName>(){

			@Override
			public FieldName apply(DataField dataField){
				return dataField.getName();
			}
		};

		MiningModel miningModel = MiningModelUtil.createClassification(function.apply(dataField), targetCategories, Lists.transform(featureMap.getDataFields(), function), models, RegressionNormalizationMethodType.SIMPLEMAX, true);

		return miningModel;
	}

	static
	private <E> List<E> getColumn(List<E> values, int index, int rows, int columns){

		if(values.size() != (rows * columns)){
			throw new IllegalArgumentException();
		}

		List<E> result = new ArrayList<>();

		for(int row = 0; row < rows; row++){
			result.add(values.get((row * columns) + index));
		}

		return result;
	}
}