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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.PMMLEncoder;

abstract
public class Classification extends ObjFunction {

	private int num_class;


	public Classification(int num_class){
		this.num_class = num_class;
	}

	@Override
	public Label encodeLabel(FieldName targetField, List<String> targetCategories, PMMLEncoder encoder){
		targetCategories = prepareTargetCategories(targetCategories);

		DataField dataField = encoder.createDataField(targetField, OpType.CATEGORICAL, DataType.STRING, targetCategories);

		return new CategoricalLabel(dataField);
	}

	private List<String> prepareTargetCategories(List<String> targetCategories){

		if(targetCategories != null){

			if(targetCategories.size() != this.num_class){
				throw new IllegalArgumentException();
			}

			return targetCategories;
		}

		List<String> result = new ArrayList<>();

		for(int i = 0; i < this.num_class; i++){
			result.add(String.valueOf(i));
		}

		return result;
	}

	public int getNumClass(){
		return this.num_class;
	}
}