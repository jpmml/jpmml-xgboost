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

import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;

abstract
public class ObjFunction {

	abstract
	public DataType getDataType();

	abstract
	public OpType getOpType();

	abstract
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, Schema schema);

	static
	public Targets createTargets(float base_score, Schema schema){

		if(!ValueUtil.isZero(base_score)){
			Target target = ModelUtil.createRescaleTarget(schema.getTargetField(), null, (double)base_score);

			Targets targets = new Targets()
				.addTargets(target);

			return targets;
		}

		return null;
	}

	static
	public Output createOutput(Transformation transformation){
		return createOutput(transformation, null);
	}

	static
	public Output createOutput(Transformation transformation, String targetCategory){
		Output output = new Output();

		String suffix = (targetCategory != null ? ("_" + targetCategory) : "");

		OutputField xgbValue = new OutputField(FieldName.create("xgbValue" + suffix), DataType.FLOAT)
			.setOpType(OpType.CONTINUOUS)
			.setResultFeature(ResultFeature.PREDICTED_VALUE)
			.setFinalResult(false);

		output.addOutputFields(xgbValue);

		if(transformation != null){
			OutputField transformedXgbValue = new OutputField(FieldName.create("transformedXgbValue" + suffix), DataType.FLOAT)
				.setOpType(OpType.CONTINUOUS)
				.setResultFeature(ResultFeature.TRANSFORMED_VALUE)
				.setFinalResult(false)
				.setExpression(transformation.createExpression(xgbValue.getName()));

			output.addOutputFields(transformedXgbValue);
		}

		return output;
	}

	static
	public interface Transformation {

		Expression createExpression(FieldName name);
	}
}