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
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.CategoricalLabel;
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

		Function<Segment, Model> function = new Function<Segment, Model>(){

			@Override
			public Model apply(Segment segment){
				return segment.getModel();
			}
		};

		List<Model> models = Lists.transform(segmentation.getSegments(), function);

		List<MiningModel> miningModels = new ArrayList<>();

		CategoricalLabel categoricalLabel = (CategoricalLabel)segmentSchema.getLabel();

		for(int i = 0, columns = categoricalLabel.size(), rows = (models.size() / columns); i < columns; i++){
			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(segmentSchema))
				.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, getColumn(models, i, rows, columns)))
				.setTargets(createTargets(base_score, segmentSchema))
				.setOutput(createOutput(SoftMaxClassification.TRANSFORMATION, categoricalLabel.getValue(i)));

			miningModels.add(miningModel);
		}

		return MiningModelUtil.createClassification(schema, miningModels, RegressionModel.NormalizationMethod.SIMPLEMAX, true);
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

	// "exp(y)"
	private static final Transformation TRANSFORMATION = new Transformation(){

		@Override
		public Expression createExpression(FieldName name){
			return PMMLUtil.createApply("exp", new FieldRef(name));
		}
	};
}