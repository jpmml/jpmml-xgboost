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

import com.google.common.primitives.Floats;
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

	public MiningModel encodeMiningModel(ObjFunction obj, float base_score, Integer ntreeLimit, Schema schema){
		RegTree[] trees = trees();
		float[] weights = tree_weights();

		return obj.encodeMiningModel(Arrays.asList(trees), weights != null ? Floats.asList(weights) : null, base_score, ntreeLimit, schema);
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