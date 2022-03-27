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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.CategoryManager;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.MissingValueFeature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ThresholdFeature;
import org.jpmml.converter.ThresholdFeatureUtil;
import org.jpmml.converter.ValueUtil;

public class RegTree implements BinaryLoadable, JSONLoadable {

	private int num_roots;

	private int num_nodes;

	private int num_deleted;

	private int max_depth;

	private int num_feature;

	private int size_leaf_vector;

	private Node[] nodes;

	private NodeStat[] stats;


	public RegTree(){
	}

	@Override
	public void loadBinary(XGBoostDataInput input) throws IOException {
		this.num_roots = input.readInt();
		this.num_nodes = input.readInt();
		this.num_deleted = input.readInt();
		this.max_depth = input.readInt();
		this.num_feature = input.readInt();
		this.size_leaf_vector = input.readInt();

		input.readReserved(31);

		this.nodes = input.readObjectArray(BinaryNode.class, this.num_nodes);
		this.stats = input.readObjectArray(BinaryNodeStat.class, this.num_nodes);
	}

	@Override
	public void loadJSON(JsonObject tree){
		JsonObject treeParam = tree.getAsJsonObject("tree_param");

		this.num_nodes = treeParam.getAsJsonPrimitive("num_nodes").getAsInt();
		this.num_deleted = treeParam.getAsJsonPrimitive("num_deleted").getAsInt();
		this.num_feature = treeParam.getAsJsonPrimitive("num_feature").getAsInt();
		this.size_leaf_vector = treeParam.getAsJsonPrimitive("size_leaf_vector").getAsInt();

		int[] parents = JSONUtil.toIntArray(tree.getAsJsonArray("parents"));
		int[] left_children = JSONUtil.toIntArray(tree.getAsJsonArray("left_children"));
		int[] right_children = JSONUtil.toIntArray(tree.getAsJsonArray("right_children"));
		boolean[] default_left = JSONUtil.toBooleanArray(tree.getAsJsonArray("default_left"));
		int[] split_indices = JSONUtil.toIntArray(tree.getAsJsonArray("split_indices"));
		int[] split_type = JSONUtil.toIntArray(tree.getAsJsonArray("split_type"));
		float[] split_conditions = JSONUtil.toFloatArray(tree.getAsJsonArray("split_conditions"));

		boolean has_cat = Ints.contains(split_type, Node.SPLIT_CATEGORICAL);

		this.nodes = new Node[this.num_nodes];

		for(int i = 0; i < this.num_nodes; i++){
			JsonObject node = new JsonObject();
			node.add("parent", new JsonPrimitive(parents[i]));
			node.add("left_child", new JsonPrimitive(left_children[i]));
			node.add("right_child", new JsonPrimitive(right_children[i]));
			node.add("default_left", new JsonPrimitive(default_left[i]));
			node.add("split_index", new JsonPrimitive(split_indices[i]));
			node.add("split_type", new JsonPrimitive(split_type[i]));
			node.add("split_condition", new JsonPrimitive(split_conditions[i]));

			this.nodes[i] = new JSONNode();
			((JSONLoadable)this.nodes[i]).loadJSON(node);
		}

		if(has_cat){
			int[] categories_segments = JSONUtil.toIntArray(tree.getAsJsonArray("categories_segments"));
			int[] categories_sizes = JSONUtil.toIntArray(tree.getAsJsonArray("categories_sizes"));
			int[] categories_nodes = JSONUtil.toIntArray(tree.getAsJsonArray("categories_nodes"));
			int[] categories = JSONUtil.toIntArray(tree.getAsJsonArray("categories"));

			int cnt = 0;

			int last_cat_node = categories_nodes[cnt];

			for(int i = 0; i < this.num_nodes; i++){
				JSONNode node = (JSONNode)this.nodes[i];

				if(i == last_cat_node){
					int j_begin = categories_segments[cnt];
					int j_end = j_begin + categories_sizes[cnt];

					int max_cat = -1;

					for(int j = j_begin; j < j_end; j++){
						int category = categories[j];

						max_cat = Math.max(max_cat, category);
					}

					if(max_cat == -1){
						throw new IllegalArgumentException();
					}

					int n_cats = (max_cat + 1);

					BitSet cat_bits = new BitSet(n_cats);

					for(int j = j_begin; j < j_end; j++){
						int category = categories[j];

						cat_bits.set(category, true);
					}

					node.set_split_categories(cat_bits);

					cnt++;

					if(cnt == categories_nodes.length){
						last_cat_node = -1;
					} else

					{
						last_cat_node = categories_nodes[cnt];
					}
				} else

				{
					node.set_split_categories(null);
				}
			}
		}
	}

	public Set<Integer> getSplitType(int splitIndex){
		Set<Integer> result = new HashSet<>();

		for(int i = 0; i < this.num_nodes; i++){
			Node node = this.nodes[i];

			if(node.split_index() == splitIndex){
				result.add(node.split_type());
			}
		}

		return result;
	}

	public Float getLeafValue(){
		Node node = this.nodes[0];

		if(node.is_leaf()){
			return node.leaf_value();
		}

		return null;
	}

	public TreeModel encodeTreeModel(boolean numeric, PredicateManager predicateManager, Schema schema){
		org.dmg.pmml.tree.Node root = encodeNode(0, True.INSTANCE, numeric, new CategoryManager(), predicateManager, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema.getLabel()), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD)
			.setMathContext(MathContext.FLOAT);

		return treeModel;
	}

	private org.dmg.pmml.tree.Node encodeNode(int index, Predicate predicate, boolean numeric, CategoryManager categoryManager, PredicateManager predicateManager, Schema schema){
		Integer id = Integer.valueOf(index);

		Node node = this.nodes[index];

		if(!node.is_leaf()){
			int splitIndex = node.split_index();

			Feature feature = schema.getFeature(splitIndex);

			boolean defaultLeft = node.default_left();

			boolean swapChildren = false;

			CategoryManager leftCategoryManager = categoryManager;
			CategoryManager rightCategoryManager = categoryManager;

			Predicate leftPredicate;
			Predicate rightPredicate;

			if(feature instanceof CategoricalFeature){

				if(node.split_type() != Node.SPLIT_CATEGORICAL){
					throw new IllegalArgumentException();
				}
			} else

			{
				if(node.split_type() != Node.SPLIT_NUMERICAL){
					throw new IllegalArgumentException();
				}
			} // End if

			if(feature instanceof CategoricalFeature){
				CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

				Float splitValue = Float.intBitsToFloat(node.split_cond());
				if(!splitValue.isNaN()){
					throw new IllegalArgumentException();
				}

				BitSet split_categories = null;

				if(node instanceof JSONNode){
					JSONNode jsonNode = (JSONNode)node;

					split_categories = jsonNode.get_split_categories();
				} // End if

				if(split_categories == null){
					throw new IllegalArgumentException();
				} else

				// Assume one-hot-encoding
				if(split_categories.cardinality() != 1){
					throw new IllegalArgumentException();
				}

				int catIndex = split_categories.nextSetBit(0);

				Object value = categoricalFeature.getValue(catIndex);

				leftPredicate = predicateManager.createSimplePredicate(categoricalFeature, SimplePredicate.Operator.NOT_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(categoricalFeature, SimplePredicate.Operator.EQUAL, value);
			} else

			if(feature instanceof BinaryFeature){
				BinaryFeature binaryFeature = (BinaryFeature)feature;

				Object value = binaryFeature.getValue();

				leftPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.NOT_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.EQUAL, value);
			} else

			if(feature instanceof MissingValueFeature){
				MissingValueFeature missingValueFeature = (MissingValueFeature)feature;

				leftPredicate = predicateManager.createSimplePredicate(missingValueFeature, SimplePredicate.Operator.IS_NOT_MISSING, null);
				rightPredicate = predicateManager.createSimplePredicate(missingValueFeature, SimplePredicate.Operator.IS_MISSING, null);
			} else

			if(feature instanceof ThresholdFeature && !numeric){
				ThresholdFeature thresholdFeature = (ThresholdFeature)feature;

				String name = thresholdFeature.getName();

				Object missingValue = thresholdFeature.getMissingValue();

				Number splitValue = Float.intBitsToFloat(node.split_cond());

				java.util.function.Predicate<Object> valueFilter = categoryManager.getValueFilter(name);

				if(!ValueUtil.isNaN(missingValue)){
					valueFilter = valueFilter.and(value -> !ValueUtil.isNaN(value));
				}

				List<Object> leftValues = thresholdFeature.getValues((Number value) -> (value.floatValue() < splitValue.floatValue())).stream()
					.filter(valueFilter)
					.collect(Collectors.toList());

				List<Object> rightValues = thresholdFeature.getValues((Number value) -> (value.floatValue() >= splitValue.floatValue())).stream()
					.filter(valueFilter)
					.collect(Collectors.toList());

				leftCategoryManager = leftCategoryManager.fork(name, leftValues);
				rightCategoryManager = rightCategoryManager.fork(name, rightValues);

				leftPredicate = ThresholdFeatureUtil.createPredicate(thresholdFeature, leftValues, missingValue, predicateManager);
				rightPredicate = ThresholdFeatureUtil.createPredicate(thresholdFeature, rightValues, missingValue, predicateManager);

				if(!ThresholdFeatureUtil.isMissingValueSafe(leftPredicate)){

					if(ThresholdFeatureUtil.isMissingValueSafe(rightPredicate)){
						swapChildren = true;
					}
				}
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				Number splitValue = Float.intBitsToFloat(node.split_cond());

				DataType dataType = continuousFeature.getDataType();
				switch(dataType){
					case INTEGER:
						Float flooredSplitValue = (float)Math.floor(splitValue.floatValue());

						if(splitValue.floatValue() == flooredSplitValue.floatValue()){
							splitValue = (int)flooredSplitValue.floatValue();
						} else

						{
							splitValue = (int)(flooredSplitValue.floatValue() + 1f);
						}
						break;
					case FLOAT:
						break;
					default:
						throw new IllegalArgumentException("Expected integer or float data type for continuous feature " + continuousFeature.getName() + ", got " + dataType.value() + " data type");
				}

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_THAN, splitValue);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_OR_EQUAL, splitValue);
			}

			org.dmg.pmml.tree.Node leftChild = encodeNode(node.left_child(), leftPredicate, numeric, leftCategoryManager, predicateManager, schema);
			org.dmg.pmml.tree.Node rightChild = encodeNode(node.right_child(), rightPredicate, numeric, rightCategoryManager, predicateManager, schema);

			org.dmg.pmml.tree.Node result = new BranchNode(null, predicate)
				.setId(id)
				.setDefaultChild(defaultLeft ? leftChild.getId() : rightChild.getId())
				.addNodes(leftChild, rightChild);

			if(swapChildren){
				List<org.dmg.pmml.tree.Node> children = result.getNodes();

				Collections.swap(children, 0, 1);
			}

			return result;
		} else

		{
			Float value = (node.leaf_value() + 0f);

			org.dmg.pmml.tree.Node result = new LeafNode(value, predicate)
				.setId(id);

			return result;
		}
	}
}