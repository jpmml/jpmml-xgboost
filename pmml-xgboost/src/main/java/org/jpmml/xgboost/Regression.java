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
import org.dmg.pmml.OpType;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelEncoder;

abstract
public class Regression extends ObjFunction {

	public Regression(String name){
		super(name);
	}

	@Override
	public Label encodeLabel(String targetName, List<?> targetCategories, ModelEncoder encoder){

		if(targetCategories != null){
			throw new IllegalArgumentException("Regression requires zero target categories");
		}

		DataField dataField = encoder.createDataField(targetName, OpType.CONTINUOUS, DataType.FLOAT);

		return new ContinuousLabel(dataField);
	}
}