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
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devsmart.ubjson.GsonUtil;
import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.google.common.primitives.Floats;
import com.google.gson.JsonObject;
import org.dmg.pmml.Model;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.converter.CMatrixUtil;
import org.jpmml.converter.Label;
import org.jpmml.converter.ScalarLabel;
import org.jpmml.converter.ScalarLabelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

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
		UBValue value = GsonUtil.toUBValue(gradientBooster);

		loadUBJSON(value.asObject());
	}

	@Override
	public void loadUBJSON(UBObject gradientBooster){
		UBObject model = gradientBooster.get("model").asObject();

		UBObject gbtreeModelParam = model.get("gbtree_model_param").asObject();

		this.num_trees = gbtreeModelParam.get("num_trees").asInt();

		if(gbtreeModelParam.containsKey("size_leaf_vector")){
			this.size_leaf_vector = gbtreeModelParam.get("size_leaf_vector").asInt();
		}

		UBArray trees = model.get("trees").asArray();

		this.trees = new RegTree[this.num_trees];

		for(int i = 0; i < this.num_trees; i++){
			UBObject tree = (trees.get(i)).asObject();

			this.trees[i] = new RegTree();
			this.trees[i].loadUBJSON(tree);
		}

		this.tree_info = UBJSONUtil.toIntArray(model.get("tree_info"));
	}

	public boolean hasCategoricalSplits(){

		for(int i = 0; i < this.num_trees; i++){
			RegTree tree = this.trees[i];

			if(tree.hasCategoricalSplits()){
				return true;
			}
		}

		return false;
	}

	public Set<Integer> getSplitType(int splitIndex){
		Set<Integer> result = new HashSet<>();

		for(int i = 0; i < this.num_trees; i++){
			RegTree tree = this.trees[i];

			result.addAll(tree.getSplitType(splitIndex));
		}

		return result;
	}

	public BitSet getSplitCategories(int splitIndex){
		BitSet result = null;

		for(int i = 0; i < this.num_trees; i++){
			RegTree tree = this.trees[i];

			BitSet splitCategories = tree.getSplitCategories(splitIndex);

			if(splitCategories != null){

				if(result == null){
					result = new BitSet();
				}

				result.or(splitCategories);
			}
		}

		return result;
	}

	public MiningModel encodeMiningModel(ObjFunction obj, float base_score, Integer ntreeLimit, boolean numeric, Schema schema){
		List<RegTree> trees = Arrays.asList(trees());
		List<Float> weights = tree_weights() != null ? Floats.asList(tree_weights()) : null;

		Label label = schema.getLabel();

		List<ScalarLabel> scalarLabels = ScalarLabelUtil.toScalarLabels(label);

		if(trees.size() % scalarLabels.size() != 0){
			throw new IllegalArgumentException();
		} // End if

		if(scalarLabels.size() == 1){
			return obj.encodeMiningModel(-1, trees, weights, base_score, ntreeLimit, numeric, schema);
		} else

		if(scalarLabels.size() >= 2){
			int rows = trees.size() / scalarLabels.size();
			int columns = scalarLabels.size();

			List<Model> models = new ArrayList<>();

			for(int i = 0; i < scalarLabels.size(); i++){
				ScalarLabel scalarLabel = scalarLabels.get(i);

				List<RegTree> segmentTrees = CMatrixUtil.getColumn(trees, rows, columns, i);
				List<Float> segmentWeights = weights != null ? CMatrixUtil.getColumn(weights, rows, columns, i) : null;

				Schema segmentSchema = schema.toRelabeledSchema(scalarLabel);

				Model model = obj.encodeMiningModel(i, segmentTrees, segmentWeights, base_score, ntreeLimit, numeric, segmentSchema);

				models.add(model);
			}

			return MiningModelUtil.createMultiModelChain(models, Segmentation.MissingPredictionTreatment.CONTINUE);
		} else

		{
			throw new IllegalArgumentException();
		}
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