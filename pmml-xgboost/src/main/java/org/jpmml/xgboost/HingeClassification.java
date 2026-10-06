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

import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.ExpressionUtil;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.MultiLabel;
import org.jpmml.converter.ScalarLabel;
import org.jpmml.converter.Schema;
import org.jpmml.converter.Transformation;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.converter.transformations.AbstractTransformation;

public class HingeClassification extends Classification implements HasMultiOutputTreeSupport {

	public HingeClassification(String name){
		super(name, 2);
	}

	@Override
	public boolean hasVectorLeafSupport(){
		return true;
	}

	@Override
	public MiningModel encodeModel(List<RegTree> trees, List<Float> weights, Float eta, float[] base_score, Integer ntreeLimit, Schema schema){
		Label label = schema.getLabel();

		Schema segmentSchema = schema.toLocalRegressorSchema("margin", DataType.FLOAT);

		Transformation transformation = createTransformation();

		if(label instanceof ScalarLabel){
			Model model = encodeOutputGroup(trees, weights, eta, base_score, ntreeLimit, segmentSchema)
				.setOutput(ModelUtil.createPredictedOutput("xgbValue", OpType.CONTINUOUS, DataType.FLOAT, transformation));

			return MiningModelUtil.createBinaryLogisticClassification(model, 1d, 0d, RegressionModel.NormalizationMethod.NONE, true, schema);
		} else

		if(label instanceof MultiLabel){
			MultiLabel multiLabel = segmentSchema.requireMultiLabel();

			Model model = encodeOutputGroup(trees, weights, eta, base_score, ntreeLimit, segmentSchema)
				.setOutput(ModelUtil.createPredictedOutput("xgbValue", OpType.CONTINUOUS, DataType.FLOAT, multiLabel, transformation));

			return MiningModelUtil.createMultiBinaryLogisticClassification(model, 1d, 0d, RegressionModel.NormalizationMethod.NONE, true, schema);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	@Override
	public MiningModel encodeMultiModel(List<List<RegTree>> targetTrees, List<List<Float>> targetWeights, Float eta, float[] base_score, Integer ntreeLimit, Schema schema){
		Transformation transformation = createTransformation();

		List<Model> models = encodeTargetOutputGroups(targetTrees, targetWeights, eta, base_score, ntreeLimit, schema, transformation);

		return MiningModelUtil.createMultiBinaryLogisticClassification(models, 1d, 0d, RegressionModel.NormalizationMethod.NONE, true, schema);
	}

	static
	private Transformation createTransformation(){
		Transformation transformation = new AbstractTransformation(){

			@Override
			public String getName(String name){
				return FieldNameUtil.create("hinge", name);
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				return ExpressionUtil.createApply(PMMLFunctions.THRESHOLD, fieldRef, ExpressionUtil.createConstant(0f));
			}
		};

		return transformation;
	}
}