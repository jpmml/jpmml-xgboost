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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.devsmart.ubjson.GsonUtil;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBValueFactory;
import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
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

public class RegTree implements BinaryLoadable, JSONLoadable, UBJSONLoadable {

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
		UBValue value = GsonUtil.toUBValue(tree);

		loadUBJSON(value.asObject());
	}

	@Override
	public void loadUBJSON(UBObject tree){
		UBObject treeParam = tree.get("tree_param").asObject();

		this.num_nodes = treeParam.get("num_nodes").asInt();
		this.num_deleted = treeParam.get("num_deleted").asInt();
		this.num_feature = treeParam.get("num_feature").asInt();
		this.size_leaf_vector = treeParam.get("size_leaf_vector").asInt();

		int[] parents = UBJSONUtil.toIntArray(tree.get("parents"));
		int[] left_children = UBJSONUtil.toIntArray(tree.get("left_children"));
		int[] right_children = UBJSONUtil.toIntArray(tree.get("right_children"));
		boolean[] default_left = UBJSONUtil.toBooleanArray(tree.get("default_left"));
		int[] split_indices = UBJSONUtil.toIntArray(tree.get("split_indices"));
		int[] split_type = UBJSONUtil.toIntArray(tree.get("split_type"));
		float[] split_conditions = UBJSONUtil.toFloatArray(tree.get("split_conditions"));

		boolean has_cat = Ints.contains(split_type, Node.SPLIT_CATEGORICAL);

		this.nodes = new Node[this.num_nodes];

		for(int i = 0; i < this.num_nodes; i++){
			UBObject node = UBValueFactory.createObject();

			node.put("parent", UBValueFactory.createInt(parents[i]));
			node.put("left_child", UBValueFactory.createInt(left_children[i]));
			node.put("right_child", UBValueFactory.createInt(right_children[i]));
			node.put("default_left", UBValueFactory.createBool(default_left[i]));
			node.put("split_index", UBValueFactory.createInt(split_indices[i]));
			node.put("split_type", UBValueFactory.createInt(split_type[i]));
			node.put("split_condition", UBValueFactory.createFloat32(split_conditions[i]));

			this.nodes[i] = new JSONNode();
			((UBJSONLoadable)this.nodes[i]).loadUBJSON(node);
		}

		if(has_cat){
			int[] categories_segments = UBJSONUtil.toIntArray(tree.get("categories_segments"));
			int[] categories_sizes = UBJSONUtil.toIntArray(tree.get("categories_sizes"));
			int[] categories_nodes = UBJSONUtil.toIntArray(tree.get("categories_nodes"));
			int[] categories = UBJSONUtil.toIntArray(tree.get("categories"));

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

	public Float getLeafValue(){
		Node node = this.nodes[0];

		if(!node.is_leaf()){
			return null;
		}

		return node.leaf_value();
	}

	public boolean hasCategoricalSplits(){

		for(int i = 0; i < this.num_nodes; i++){
			Node node = this.nodes[i];

			if(!node.is_leaf()){

				if(node.split_type() == Node.SPLIT_CATEGORICAL){
					return true;
				}
			}
		}

		return false;
	}

	public Set<Integer> getSplitType(int splitIndex){
		Set<Integer> result = new HashSet<>();

		for(int i = 0; i < this.num_nodes; i++){
			Node node = this.nodes[i];

			if(!node.is_leaf()){

				if(node.split_index() == splitIndex){
					result.add(node.split_type());
				}
			}
		}

		return result;
	}

	public BitSet getSplitCategories(int splitIndex){
		BitSet result = null;

		for(int i = 0; i < this.num_nodes; i++){
			Node node = this.nodes[i];

			if(!node.is_leaf()){

				if(node.split_index() == splitIndex){
					BitSet splitCategories = node.get_split_categories();

					if(splitCategories != null){

						if(result == null){
							result = new BitSet();
						}

						result.or(splitCategories);
					}
				}
			}
		}

		return result;
	}

	public TreeModel encodeTreeModel(PredicateManager predicateManager, Schema schema){
		org.dmg.pmml.tree.Node root = encodeNode(0, True.INSTANCE, new CategoryManager(), predicateManager, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema.getLabel()), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD)
			.setMathContext(MathContext.FLOAT);

		return treeModel;
	}

	private org.dmg.pmml.tree.Node encodeNode(int index, Predicate predicate, CategoryManager categoryManager, PredicateManager predicateManager, Schema schema){
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
				CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

				if(node.split_type() != Node.SPLIT_CATEGORICAL){
					throw new IllegalArgumentException("Expected a categorical (" + Node.SPLIT_CATEGORICAL + ") split type for categorical feature \'" + categoricalFeature.getName() + "\', got non-categorical (" + node.split_type() + ")");
				}
			} else

			{
				if(node.split_type() != Node.SPLIT_NUMERICAL){
					throw new IllegalArgumentException("Expected a numerical (" + Node.SPLIT_NUMERICAL + ") split type for feature \'" + feature.getName() + "\', got non-numerical (" + node.split_type() +")");
				}
			} // End if

			if(feature instanceof CategoricalFeature){
				CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

				String name = categoricalFeature.getName();
				List<?> values = categoricalFeature.getValues();

				Float splitValue = Float.intBitsToFloat(node.split_cond());
				if(!splitValue.isNaN()){
					throw new IllegalArgumentException();
				}

				BitSet split_categories = node.get_split_categories();
				if(split_categories == null){
					throw new IllegalArgumentException();
				}

				java.util.function.Predicate<Object> valueFilter = categoryManager.getValueFilter(name);

				List<Object> leftValues = new ArrayList<>();
				List<Object> rightValues = new ArrayList<>();

				for(int i = 0; i < values.size(); i++){
					Object value = values.get(i);

					if(!valueFilter.test(value)){
						continue;
					} // End if

					if(!split_categories.get(i)){
						leftValues.add(value);
					} else

					{
						rightValues.add(value);
					}
				}

				leftCategoryManager = leftCategoryManager.fork(name, leftValues);
				rightCategoryManager = rightCategoryManager.fork(name, rightValues);

				leftPredicate = predicateManager.createPredicate(categoricalFeature, leftValues);
				rightPredicate = predicateManager.createPredicate(categoricalFeature, rightValues);
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

			if(feature instanceof ThresholdFeature){
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
					case DOUBLE:
						continuousFeature = continuousFeature.toContinuousFeature(DataType.FLOAT);
						break;
					default:
						throw new IllegalArgumentException("Expected integer or floating-point data type for continuous feature \'" + continuousFeature.getName() + "\', got " + dataType.value() + " data type");
				}

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_THAN, splitValue);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_OR_EQUAL, splitValue);
			}

			org.dmg.pmml.tree.Node leftChild = encodeNode(node.left_child(), leftPredicate, leftCategoryManager, predicateManager, schema);
			org.dmg.pmml.tree.Node rightChild = encodeNode(node.right_child(), rightPredicate, rightCategoryManager, predicateManager, schema);

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