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

import com.google.gson.JsonArray;
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
		JsonObject gbtree = gradientBooster.getAsJsonObject("gbtree");

		super.loadJSON(gbtree);

		JsonArray weightDrop = gradientBooster.getAsJsonArray("weight_drop");

		this.weight_drop = JSONUtil.toFloatArray(weightDrop);
	}

	@Override
	public float[] tree_weights(){
		return this.weight_drop;
	}
}