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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.primitives.Floats;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.Schema;

public class GBTree extends GradientBooster {

	private int num_trees;

	private int num_roots;

	private int num_feature;

	private int num_output_group;

	private int size_leaf_vector;

	private RegTree[] trees;

	private int[] tree_info;


	public GBTree(){
	}

	@Override
	public String getAlgorithmName(){
		return "GBTree";
	}

	@Override
	public void loadBinary(XGBoostDataInput input) throws IOException {
		this.num_trees = input.readInt();
		this.num_roots = input.readInt();
		this.num_feature = input.readInt();

		input.readReserved(3);

		this.num_output_group = input.readInt();
		this.size_leaf_vector = input.readInt();

		input.readReserved(32);

		this.trees = input.readObjectArray(RegTree.class, this.num_trees);
		this.tree_info = input.readIntArray(this.num_trees);
	}

	@Override
	public void loadJSON(JsonObject gradientBooster){
		JsonObject model = gradientBooster.getAsJsonObject("model");

		JsonObject gbtreeModelParam = model.getAsJsonObject("gbtree_model_param");

		this.num_trees = gbtreeModelParam.getAsJsonPrimitive("num_trees").getAsInt();
		this.size_leaf_vector = gbtreeModelParam.getAsJsonPrimitive("size_leaf_vector").getAsInt();

		JsonArray trees = model.getAsJsonArray("trees");

		this.trees = new RegTree[this.num_trees];

		for(int i = 0; i < this.num_trees; i++){
			JsonObject tree = (trees.get(i)).getAsJsonObject();

			this.trees[i] = new RegTree();
			this.trees[i].loadJSON(tree);
		}

		this.tree_info = JSONUtil.toIntArray(model.getAsJsonArray("tree_info"));
	}

	public Set<Integer> getSplitType(int splitIndex){
		Set<Integer> result = new HashSet<>();

		for(int i = 0; i < this.num_trees; i++){
			RegTree tree = this.trees[i];

			result.addAll(tree.getSplitType(splitIndex));
		}

		return result;
	}

	public MiningModel encodeMiningModel(ObjFunction obj, float base_score, Integer ntreeLimit, boolean numeric, Schema schema){
		RegTree[] trees = trees();
		float[] weights = tree_weights();

		return obj.encodeMiningModel(Arrays.asList(trees), weights != null ? Floats.asList(weights) : null, base_score, ntreeLimit, numeric, schema);
	}

	public int num_trees(){
		return this.num_trees;
	}

	public RegTree[] trees(){
		return this.trees;
	}

	public float[] tree_weights(){
		return null;
	}
}