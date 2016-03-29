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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

abstract
public class Classification extends ObjFunction {

	private List<String> targetCategories = null;


	public Classification(int num_class){
		super(OpType.CATEGORICAL, DataType.STRING);

		setTargetCategories(createTargetCategories(num_class));
	}

	public void updateTargetCategories(List<String> targetCategories){

		if((this.targetCategories != null) && (this.targetCategories.size() != targetCategories.size())){
			throw new IllegalArgumentException();
		}

		this.targetCategories = targetCategories;
	}

	public List<String> getTargetCategories(){
		return this.targetCategories;
	}

	private void setTargetCategories(List<String> targetCategories){
		this.targetCategories = targetCategories;
	}

	static
	private List<String> createTargetCategories(int size){
		List<String> result = new ArrayList<>();

		for(int i = 0; i < size; i++){
			result.add(String.valueOf(i));
		}

		return result;
	}
}