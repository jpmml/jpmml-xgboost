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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FeatureType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Segmentation;

abstract
public class ObjFunction {

	private DataField dataField = null;


	public ObjFunction(OpType opType, DataType dataType){
		DataField dataField = new DataField(FieldName.create("_target"), opType, dataType);

		setDataField(dataField);
	}

	abstract
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, FeatureMap featureMap);

	public DataField getDataField(){
		return this.dataField;
	}

	private void setDataField(DataField dataField){
		this.dataField = dataField;
	}

	static
	public OutputField createPredictedField(FieldName name){
		OutputField outputField = new OutputField(name)
			.setFeature(FeatureType.PREDICTED_VALUE);

		return outputField;
	}

	static
	public OutputField createTransformedField(FieldName name, Expression expression){
		OutputField outputField = new OutputField(name)
			.setFeature(FeatureType.TRANSFORMED_VALUE)
			.setDataType(DataType.FLOAT)
			.setOpType(OpType.CONTINUOUS)
			.setExpression(expression);

		return outputField;
	}
}