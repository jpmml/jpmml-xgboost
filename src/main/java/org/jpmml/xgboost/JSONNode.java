/*
 * Copyright (c) 2021 Villu Ruusmann
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

import com.google.gson.JsonObject;

public class JSONNode extends Node implements JSONLoadable {

	private int parent;

	private int left_child;

	private int right_child;

	private boolean default_left;

	private int split_index;

	private float split_condition;


	public JSONNode(){
	}

	@Override
	public void loadJSON(JsonObject node){
		this.parent = node.getAsJsonPrimitive("parent").getAsInt();
		this.left_child = node.getAsJsonPrimitive("left_child").getAsInt();
		this.right_child = node.getAsJsonPrimitive("right_child").getAsInt();
		this.default_left = node.getAsJsonPrimitive("default_left").getAsBoolean();
		this.split_index = node.getAsJsonPrimitive("split_index").getAsInt();
		this.split_condition = node.getAsJsonPrimitive("split_condition").getAsFloat();
	}

	@Override
	public boolean is_leaf(){
		return (this.left_child == -1);
	}

	@Override
	public int left_child(){
		return this.left_child;
	}

	@Override
	public int right_child(){
		return this.right_child;
	}

	@Override
	public boolean default_left(){
		return this.default_left;
	}

	@Override
	public int split_index(){
		return this.split_index;
	}

	@Override
	public int split_cond(){
		return Float.floatToIntBits(this.split_condition);
	}

	@Override
	public float leaf_value(){
		return this.split_condition;
	}
}