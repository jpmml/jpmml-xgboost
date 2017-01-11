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
import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.Schema;

public class GBTree {

	private int num_trees;

	private int num_roots;

	private int num_feature;

	private int num_output_group;

	private int size_leaf_vector;

	private List<RegTree> trees;

	private int[] tree_info;


	public GBTree(){
	}

	public void load(XGBoostDataInput input) throws IOException {
		this.num_trees = input.readInt();
		this.num_roots = input.readInt();
		this.num_feature = input.readInt();

		input.readReserved(3);

		this.num_output_group = input.readInt();
		this.size_leaf_vector = input.readInt();

		input.readReserved(32);

		this.trees = new ArrayList<>();

		for(int i = 0; i < this.num_trees; i++){
			RegTree tree = new RegTree();
			tree.load(input);

			this.trees.add(tree);
		}

		this.tree_info = new int[this.num_trees];

		for(int i = 0; i < this.num_trees; i++){
			this.tree_info[i] = input.readInt();
		}
	}

	public MiningModel encodeMiningModel(ObjFunction obj, float base_score, Schema schema){
		return obj.encodeMiningModel(this.trees, base_score, schema);
	}

	public List<RegTree> getTrees(){
		return this.trees;
	}
}