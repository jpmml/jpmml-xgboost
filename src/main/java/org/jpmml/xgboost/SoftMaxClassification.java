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

import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class SoftMaxClassification extends Classification {

	public SoftMaxClassification(int num_class){
		super(num_class);

		if(num_class < 3){
			throw new IllegalArgumentException("Multi-class classification requires three or more target categories");
		}
	}

	@Override
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, Schema schema){
		Schema segmentSchema = schema.toAnonymousSchema();

		List<Segment> segments = segmentation.getSegments();

		List<MiningModel> models = new ArrayList<>();

		List<String> targetCategories = schema.getTargetCategories();
		for(int i = 0; i < targetCategories.size(); i++){
			String targetCategory = targetCategories.get(i);

			Output valueOutput = encodeOutput(targetCategory);

			List<Segment> valueSegments = getColumn(segments, i, (segments.size() / targetCategories.size()), targetCategories.size());

			Segmentation valueSegmentation = new Segmentation(Segmentation.MultipleModelMethod.SUM, valueSegments);

			MiningModel valueMiningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(segmentSchema))
				.setSegmentation(valueSegmentation)
				.setTargets(createTargets(base_score, segmentSchema))
				.setOutput(valueOutput);

			models.add(valueMiningModel);
		}

		return MiningModelUtil.createClassification(schema, models, RegressionModel.NormalizationMethod.SIMPLEMAX, true);
	}

	static
	private Output encodeOutput(String targetCategory){
		OutputField xgbValue = createPredictedField(FieldName.create("xgbValue_" + targetCategory));

		Expression expression = PMMLUtil.createApply("exp", new FieldRef(xgbValue.getName()));

		OutputField transformedXgbValue = createTransformedField(FieldName.create("transformedXgbValue_" + targetCategory), expression);

		Output output = new Output()
			.addOutputFields(xgbValue, transformedXgbValue);

		return output;
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