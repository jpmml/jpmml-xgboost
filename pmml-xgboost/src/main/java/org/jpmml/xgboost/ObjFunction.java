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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;
import org.jpmml.converter.mining.MiningModelUtil;

abstract
public class ObjFunction {

	private String name;


	public ObjFunction(String name){
		this.name = name;
	}

	abstract
	public Label encodeLabel(String targetName, List<?> targetCategories, PMMLEncoder encoder);

	abstract
	public MiningModel encodeMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, boolean numeric, Schema schema);

	public MiningModel encodeMiningModel(int targetIndex, List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, boolean numeric, Schema schema){
		return encodeMiningModel(trees, weights, base_score, ntreeLimit, numeric, schema);
	}

	public float probToMargin(float value){
		return value;
	}

	public String getName(){
		return this.name;
	}

	static
	protected MiningModel createMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, boolean numeric, Schema schema){
		trees = new ArrayList<>(trees);

		if(weights != null){
			weights = new ArrayList<>(weights);

			if(trees.size() != weights.size()){
				throw new IllegalArgumentException();
			}
		} // End if

		if(ntreeLimit != null){

			if(ntreeLimit > trees.size()){
				throw new IllegalArgumentException("Tree limit " + ntreeLimit + " is greater than the number of trees");
			}

			trees = trees.subList(0, ntreeLimit);

			if(weights != null){
				weights = weights.subList(0, ntreeLimit);
			}
		}

		ContinuousLabel continuousLabel = (ContinuousLabel)schema.getLabel();

		Schema segmentSchema = schema.toAnonymousSchema();

		PredicateManager predicateManager = new PredicateManager();

		List<TreeModel> treeModels = new ArrayList<>();

		Number intercept = base_score;

		boolean equalWeights = true;

		// First filtering pass - eliminating empty trees
		{
			Iterator<RegTree> treeIt = trees.iterator();
			Iterator<Float> weightIt = (weights != null ? weights.iterator() : null);

			while(treeIt.hasNext()){
				RegTree tree = treeIt.next();
				Float weight = (weightIt != null ? weightIt.next() : null);

				Float leafValue = tree.getLeafValue();
				if(leafValue != null && ValueUtil.isZero(leafValue)){
					treeIt.remove();

					if(weightIt != null){
						weightIt.remove();
					}

					continue;
				} // End if

				if(weight != null){
					equalWeights &= ValueUtil.isOne(weight);
				}
			}
		}

		// Second filtering pass - eliminating constant-prediction trees
		if(equalWeights){
			Iterator<RegTree> treeIt = trees.iterator();
			Iterator<Float> weightIt = (weights != null ? weights.iterator() : null);

			while(treeIt.hasNext()){
				RegTree tree = treeIt.next();
				Float weight = (weightIt != null ? weightIt.next() : null);

				Float leafValue = tree.getLeafValue();
				if(leafValue != null){
					intercept = ValueUtil.add(MathContext.FLOAT, intercept, leafValue);

					treeIt.remove();

					if(weightIt != null){
						weightIt.remove();
					}
				}
			}
		}

		// Final pass - encoding trees
		{
			for(RegTree tree : trees){
				TreeModel treeModel = tree.encodeTreeModel(numeric, predicateManager, segmentSchema);

				treeModels.add(treeModel);
			}
		}

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
			.setMathContext(MathContext.FLOAT)
			.setSegmentation(MiningModelUtil.createSegmentation(equalWeights ? Segmentation.MultipleModelMethod.SUM : Segmentation.MultipleModelMethod.WEIGHTED_SUM, Segmentation.MissingPredictionTreatment.RETURN_MISSING, treeModels, weights))
			.setTargets(ModelUtil.createRescaleTargets(null, intercept, continuousLabel));

		return miningModel;
	}

	static
	protected float inverseLogit(float value){
		return (float)-Math.log((1f / value) - 1f);
	}

	static
	protected float inverseExp(float value){
		return (float)Math.log(value);
	}
}