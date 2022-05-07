/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.io.IOException;

import com.devsmart.ubjson.GsonUtil;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.google.gson.JsonObject;

public class Dart extends GBTree {

	private float[] weight_drop;


	public Dart(){
	}

	@Override
	public String getAlgorithmName(){
		return "DART";
	}

	@Override
	public void loadBinary(XGBoostDataInput input) throws IOException {
		super.loadBinary(input);

		int num_trees = num_trees();
		if(num_trees != 0){
			this.weight_drop = input.readFloatVector();
		}
	}

	@Override
	public void loadJSON(JsonObject gradientBooster){
		UBValue value = GsonUtil.toUBValue(gradientBooster);

		loadUBJSON(value.asObject());
	}

	@Override
	public void loadUBJSON(UBObject gradientBooster){
		UBObject gbtree = gradientBooster.get("gbtree").asObject();

		super.loadUBJSON(gbtree);

		this.weight_drop = UBJSONUtil.toFloatArray(gradientBooster.get("weight_drop"));
	}

	@Override
	public float[] tree_weights(){
		return this.weight_drop;
	}
}