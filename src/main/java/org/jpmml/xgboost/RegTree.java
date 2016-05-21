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

import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.MissingValueStrategyType;
import org.dmg.pmml.TreeModel;
import org.dmg.pmml.True;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.ValueUtil;

public class RegTree {

	private int num_roots;

	private int num_nodes;

	private int num_deleted;

	private int max_depth;

	private int num_feature;

	private int size_leaf_vector;

	private List<Node> nodes;

	private List<NodeStat> stats;


	public RegTree(){
	}

	public void load(XGBoostDataInput input) throws IOException {
		this.num_roots = input.readInt();
		this.num_nodes = input.readInt();
		this.num_deleted = input.readInt();
		this.max_depth = input.readInt();
		this.num_feature = input.readInt();
		this.size_leaf_vector = input.readInt();

		input.readReserved(31);

		this.nodes = new ArrayList<>();

		for(int i = 0; i < this.num_nodes; i++){
			Node node = new Node();
			node.load(input);

			this.nodes.add(node);
		}

		this.stats = new ArrayList<>();

		for(int i = 0; i < this.num_nodes; i++){
			NodeStat stat = new NodeStat();
			stat.load(input);

			this.stats.add(stat);
		}
	}

	public TreeModel encodeTreeModel(FeatureMap featureMap){
		org.dmg.pmml.Node root = new org.dmg.pmml.Node()
			.setPredicate(new True());

		encodeNode(root, 0, featureMap);

		MiningSchema miningSchema = ModelUtil.createMiningSchema(null, featureMap.getDataFields(), root);

		TreeModel treeModel = new TreeModel(MiningFunctionType.REGRESSION, miningSchema, root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(MissingValueStrategyType.DEFAULT_CHILD);

		return treeModel;
	}

	private void encodeNode(org.dmg.pmml.Node parent, int index, FeatureMap featureMap){
		parent.setId(String.valueOf(index + 1));

		Node node = this.nodes.get(index);

		if(!node.is_leaf()){
			int splitIndex = node.split_index();

			Feature feature = featureMap.getFeature(splitIndex);

			int splitCondition = node.split_cond();

			org.dmg.pmml.Node leftChild = new org.dmg.pmml.Node()
				.setPredicate(feature.encodePredicate(splitCondition, true));

			encodeNode(leftChild, node.cleft(), featureMap);

			org.dmg.pmml.Node rightChild = new org.dmg.pmml.Node()
				.setPredicate(feature.encodePredicate(splitCondition, false));

			parent.addNodes(leftChild, rightChild);

			encodeNode(rightChild, node.cright(), featureMap);

			boolean defaultLeft = feature.isDefaultLeft(node);

			parent.setDefaultChild(defaultLeft ? leftChild.getId() : rightChild.getId());
		} else

		{
			float value = node.leaf_value();

			parent.setScore(ValueUtil.formatValue(value));
		}
	}

	public List<Node> getNodes(){
		return this.nodes;
	}
}