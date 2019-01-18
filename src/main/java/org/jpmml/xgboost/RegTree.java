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

import org.dmg.pmml.DataType;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
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

	public TreeModel encodeTreeModel(PredicateManager predicateManager, Schema schema){
		org.dmg.pmml.tree.Node root = new org.dmg.pmml.tree.ComplexNode()
			.setPredicate(new True());

		encodeNode(root, predicateManager, 0, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema.getLabel()), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD)
			.setMathContext(MathContext.FLOAT);

		return treeModel;
	}

	private void encodeNode(org.dmg.pmml.tree.Node parent, PredicateManager predicateManager, int index, Schema schema){
		parent.setId(String.valueOf(index + 1));

		Node node = this.nodes.get(index);

		if(!node.is_leaf()){
			int splitIndex = node.split_index();

			Feature feature = schema.getFeature(splitIndex);

			Predicate leftPredicate;
			Predicate rightPredicate;

			boolean defaultLeft;

			if(feature instanceof BinaryFeature){
				BinaryFeature binaryFeature = (BinaryFeature)feature;

				String value = binaryFeature.getValue();

				leftPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.NOT_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.EQUAL, value);

				defaultLeft = true;
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				Number splitValue = Float.intBitsToFloat(node.split_cond());

				DataType dataType = continuousFeature.getDataType();
				switch(dataType){
					case INTEGER:
						splitValue = (int)(splitValue.floatValue() + 1f);
						break;
					case FLOAT:
						break;
					default:
						throw new IllegalArgumentException();
				}

				String value = ValueUtil.formatValue(splitValue);

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_THAN, value);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_OR_EQUAL, value);

				defaultLeft = node.default_left();
			}

			org.dmg.pmml.tree.Node leftChild = new org.dmg.pmml.tree.ComplexNode()
				.setPredicate(leftPredicate);

			encodeNode(leftChild, predicateManager, node.cleft(), schema);

			org.dmg.pmml.tree.Node rightChild = new org.dmg.pmml.tree.ComplexNode()
				.setPredicate(rightPredicate);

			encodeNode(rightChild, predicateManager, node.cright(), schema);

			parent.addNodes(leftChild, rightChild);

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