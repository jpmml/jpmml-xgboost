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

import java.util.BitSet;

import com.devsmart.ubjson.GsonUtil;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.google.gson.JsonObject;

public class JSONNode extends Node implements JSONLoadable, UBJSONLoadable {

	private int parent;

	private int left_child;

	private int right_child;

	private boolean default_left;

	private int split_index;

	private int split_type;

	private float split_condition;

	private BitSet split_categories;


	public JSONNode(){
	}

	@Override
	public void loadJSON(JsonObject node){
		UBValue value = GsonUtil.toUBValue(node);

		loadUBJSON(value.asObject());
	}

	@Override
	public void loadUBJSON(UBObject node){
		this.parent = node.get("parent").asInt();
		this.left_child = node.get("left_child").asInt();
		this.right_child = node.get("right_child").asInt();
		this.default_left = node.get("default_left").asBool();
		this.split_index = node.get("split_index").asInt();
		this.split_type = node.get("split_type").asInt();
		this.split_condition = node.get("split_condition").asFloat32();

		switch(this.split_type){
			case Node.SPLIT_NUMERICAL:
			case Node.SPLIT_CATEGORICAL:
				break;
			default:
				throw new IllegalArgumentException();
		}
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
	public int split_type(){
		return this.split_type;
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

	@Override
	public BitSet get_split_categories(){
		return this.split_categories;
	}

	void set_split_categories(BitSet split_categories){
		this.split_categories = split_categories;
	}
}