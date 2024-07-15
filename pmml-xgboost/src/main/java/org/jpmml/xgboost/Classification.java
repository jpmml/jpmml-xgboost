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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.FieldNames;
import org.jpmml.converter.Label;
import org.jpmml.converter.LabelUtil;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

abstract
public class Classification extends ObjFunction {

	private int num_class;


	public Classification(String name, int num_class){
		super(name);

		this.num_class = num_class;
	}

	@Override
	public Label encodeLabel(String targetName, List<?> targetCategories, ModelEncoder encoder){
		DataField dataField;

		if(targetCategories == null){
			targetCategories = LabelUtil.createTargetCategories(this.num_class);

			dataField = encoder.createDataField(targetName, OpType.CATEGORICAL, DataType.INTEGER, targetCategories);
		} else

		{
			if(targetCategories.size() != this.num_class){
				throw new IllegalArgumentException("Expected " + this.num_class + " target categories, got " + targetCategories.size() + " target categories");
			}

			dataField = encoder.createDataField(targetName, OpType.CATEGORICAL, DataType.STRING, targetCategories);
		}

		return new CategoricalLabel(dataField);
	}

	@Override
	public MiningModel encodeModel(int targetIndex, List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, Schema schema){
		MiningModel miningModel = encodeModel(trees, weights, base_score, ntreeLimit, schema);

		if(targetIndex != -1){
			Model finalModel = MiningModelUtil.getFinalModel(miningModel);

			Output output = finalModel.getOutput();
			if(output == null || !output.hasOutputFields()){
				throw new IllegalArgumentException();
			}

			List<OutputField> outputFields = output.getOutputFields();

			outputFields.removeIf((outputField) -> {
				return (outputField.getResultFeature() == ResultFeature.PROBABILITY);
			});

			CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

			List<?> values = categoricalLabel.getValues();

			values.stream()
				.map(value -> {
					return ModelUtil.createProbabilityField(FieldNameUtil.create(FieldNames.PROBABILITY, categoricalLabel.getName(), value), DataType.FLOAT, value);
				})
				.forEach(outputFields::add);
		}

		return miningModel;
	}

	public int num_class(){
		return this.num_class;
	}
}